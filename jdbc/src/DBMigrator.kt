package klite.jdbc

import klite.info
import klite.jdbc.ChangeSet.OnChange
import klite.jdbc.ChangeSet.OnChange.*
import klite.logger
import klite.warn
import java.io.Reader
import java.lang.Thread.currentThread
import java.sql.SQLException
import javax.sql.DataSource

/** Work in progress replacement for Liquibase SQL format */
open class DBMigrator(
  private val db: DataSource,
  private val filePaths: List<String> = listOf("db.sql"),
  private val substitutions: Map<String, String> = emptyMap()
) {
  private val log = logger()
  private val commentRegex = "\\s*--.*".toRegex()
  private val substRegex = "\\\$\\{(\\w*)}".toRegex()
  private val repository = ChangeSetRepository(db)

  private val tx = Transaction(db)
  private var executed: Map<String, ChangeSet> = emptyMap()

  fun migrate() = tx.attachToThread().use {
    executed = readExecuted()
    filePaths.forEach { migrateFile(it) }
  }

  private fun readExecuted() = (try { repository.list() } catch (e: SQLException) {
    tx.rollback()
    migrateFile("db_changelog.sql")
    repository.list()
  }).associateBy { it.id }

  open fun readFile(path: String) = currentThread().contextClassLoader.getResourceAsStream(path) ?: throw MigrationException("$path not found in classpath")
  fun migrateFile(path: String) = readFile(path).reader().use { migrate(path, it) }

  private fun migrate(path: String, reader: Reader) {
    log.info("Reading $path")
    var changeSet = ChangeSet("")
    reader.buffered().lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.forEach { line ->
      if (line.startsWith("--include")) {
        exec(changeSet)
        migrateFile(line.substringAfter("--include").trim())
      }
      else if (line.startsWith("--changeset")) {
        exec(changeSet)
        val parts = line.split("\\s+".toRegex())
        val args = parts.drop(2).associate { it.split(":", limit = 2).let { it[0] to it[1] } }
        changeSet = ChangeSet(parts[1], filePath = path,
          context = args["context"], separator = args["separator"] ?: ";",
          onChange = args["onChange"]?.let { OnChange.valueOf(it) } ?: FAIL)
        // TODO: use Constructor.callBy() to fail on unsupported params or create Map.fromValues()
      }
      else changeSet.addLine(line.replace(commentRegex, "").substitute())
    }
    exec(changeSet)
  }

  private fun String.substitute() = substRegex.replace(this) { substitutions[it.groupValues[1]] ?: error("Unknown substitution ${it.value}") }

  fun exec(changeSet: ChangeSet) {
    if (changeSet.id.isEmpty()) {
      if (changeSet.sql.isNotEmpty())
        throw MigrationException("Cannot execute dangling SQL without a changeset:\n" + changeSet.sql)
      return
    }
    var run = true
    executed[changeSet.id]?.let {
      if (it.checksum == changeSet.checksum) return
      when (changeSet.onChange) {
        FAIL -> throw MigrationException(changeSet, "has changed, old checksum=${it.checksum}, use onChange to override")
        SKIP -> return log.warn("Skipping changed $changeSet")
        MARK_RAN -> {
          log.warn("Marking changed $changeSet as ran")
          run = false
        }
        RUN -> {}
      }
    }
    try {
      if (run) changeSet.statements.forEach {
        log.info("Executing ${changeSet.copy(sql = it)}")
        db.exec(it)
      }
      repository.save(changeSet)
      tx.commit()
    } catch (e: Exception) {
      tx.rollback()
      throw MigrationException(changeSet, e)
    }
  }
}

data class ChangeSet(
  override val id: String,
  val sql: CharSequence = StringBuilder(),
  val context: String? = null,
  val onChange: OnChange = FAIL,
  val separator: String? = ";",
  val filePath: String? = null,
  var checksum: Long? = null
): BaseEntity<String> {
  val statements = mutableListOf<String>()

  private var lastPos = 0
  internal fun addLine(line: String) {
    if ((sql as StringBuilder).isNotEmpty()) sql.append("\n")
    sql.append(line)
    if (separator != null) {
      val pos = sql.indexOf(separator, lastPos)
      if (pos >= 0) {
        val stmt = sql.substring(lastPos, pos)
        statements += stmt
        checksum = (checksum ?: 0) * 89 + stmt.replace("\\s*\n\\s*".toRegex(), "\n").hashCode()
        lastPos = pos + separator.length
      }
    }
  }

  enum class OnChange { FAIL, RUN, SKIP, MARK_RAN }
}

class ChangeSetRepository(db: DataSource): BaseCrudRepository<ChangeSet, String>(db, "db_changelog") {
  override val defaultOrder = "$orderAsc for update"
  override fun ChangeSet.persister() = toValuesSkipping(ChangeSet::separator, ChangeSet::sql, ChangeSet::onChange)
}

class MigrationException(message: String, cause: Throwable? = null): RuntimeException(message, cause) {
  constructor(changeSet: ChangeSet, cause: Throwable? = null): this("Failed $changeSet", cause)
  constructor(changeSet: ChangeSet, message: String): this("$changeSet\n - $message")
}

package klite.jdbc

import klite.*
import klite.jdbc.ChangeSet.OnChange.*
import org.intellij.lang.annotations.Language
import java.io.Reader
import java.lang.Thread.currentThread
import java.sql.SQLException
import javax.sql.DataSource

/** Work in progress replacement for Liquibase SQL format */
open class DBMigrator(
  private val db: DataSource,
  private val filePaths: List<String> = listOf(Config.optional("DB_MIGRATE", "db.sql")),
  private val contexts: Set<String> = Config.active,
  private val substitutions: MutableMap<String, String> = mutableMapOf()
): Extension {
  private val log = logger()
  private val commentRegex = "\\s*--.*".toRegex()
  private val substRegex = "\\\$\\{(\\w*)}".toRegex()
  private val repository = ChangeSetRepository(db)

  private val tx = Transaction(db)
  private var history: Map<String, ChangeSet> = emptyMap()

  override fun install(server: Server) = migrate()

  fun migrate() = tx.attachToThread().use {
    history = readHistory()
    filePaths.forEach { migrateFile(it) }
  }

  private fun readHistory() = (try { repository.list() } catch (e: SQLException) {
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
        run(changeSet)
        migrateFile(line.substringAfter("--include").trim())
      }
      else if (line.startsWith("--substitute")) {
        run(changeSet)
        val (key, value) = line.substringAfter("--substitute").trim().split('=', limit = 2)
        substitutions[key] = value
      }
      else if (line.startsWith("--changeset")) {
        run(changeSet)
        val parts = line.split("\\s+".toRegex())
        val args = mapOf(ChangeSet::id.name to parts[1], ChangeSet::filePath.name to path) +
          parts.drop(2).associate { it.split(":", limit = 2).let { it[0] to it[1] } }
        changeSet = args.fromValues()
      }
      else changeSet.addLine(line.replace(commentRegex, "").substitute())
    }
    run(changeSet)
  }

  private fun String.substitute() = substRegex.replace(this) { substitutions[it.groupValues[1]] ?: error("Unknown substitution ${it.value}") }

  fun run(changeSet: ChangeSet) {
    changeSet.finish()
    if (changeSet.id.isEmpty()) {
      if (changeSet.sql.isNotEmpty())
        throw MigrationException("Cannot run dangling SQL without a changeset:\n" + changeSet.sql)
      return
    }
    if (changeSet.context != null && changeSet.context !in contexts) return
    var run = true
    history[changeSet.id]?.let {
      if (it.checksum == changeSet.checksum) return
      when (changeSet.onChange) {
        FAIL -> throw MigrationException(changeSet, "has changed, old checksum=${it.checksum}, use onChange to override")
        SKIP -> return log.warn("Skipping changed $changeSet")
        MARK_RAN -> {
          log.warn("$changeSet - marking as ran")
          run = false
        }
        RUN -> {}
      }
    }
    try {
      if (run) changeSet.statements.forEach {
        log.info("Running ${changeSet.copy(sql = it)}")
        changeSet.rowsAffected += db.exec(it)
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
  @Language("SQL") val sql: CharSequence = StringBuilder(),
  val context: String? = null,
  val onChange: OnChange = FAIL,
  val separator: String? = ";",
  val filePath: String? = null,
  var checksum: Long? = null
): BaseEntity<String> {
  val statements = mutableListOf<String>()
  var rowsAffected = 0

  private var lastPos = 0
  internal fun addLine(line: String) {
    sql as StringBuilder
    if (sql.isNotEmpty()) sql.append("\n")
    sql.append(line)
    if (separator != null) addNextStatement()
  }

  private fun addNextStatement(pos: Int = sql.indexOf(separator ?: "", lastPos)) {
    if (pos <= lastPos) return
    val stmt = sql.substring(lastPos, pos)
    statements += stmt
    checksum = (checksum ?: 0) * 89 + stmt.replace("\\s*\n\\s*".toRegex(), "\n").hashCode()
    lastPos = pos + (separator ?: "").length
  }

  internal fun finish() = addNextStatement(sql.length)

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

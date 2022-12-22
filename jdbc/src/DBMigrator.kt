package klite.jdbc

import klite.info
import klite.logger
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
  private val commentRegex = "--.*".toRegex()
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
    var changeSet = ChangeSet("", path)
    reader.buffered().lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.forEach { line ->
      if (line.startsWith("--include")) {
        exec(changeSet)
        migrateFile(line.substringAfter("--include").trim())
      }
      else if (line.startsWith("--changeset")) {
        exec(changeSet)
        val parts = line.split("\\s+".toRegex())
        changeSet = ChangeSet(parts[1], path)
      }
      else changeSet += line.replace(commentRegex, "").substitute()
    }
    exec(changeSet)
  }

  private fun String.substitute() = substRegex.replace(this) { substitutions[it.groupValues[1]] ?: error("Unknown substitution ${it.value}") }

  fun exec(changeSet: ChangeSet) {
    if (changeSet.id.isEmpty()) {
      if (changeSet.sql.isNotEmpty()) error("Cannot execute dangling SQL without a changeset:\n" + changeSet.sql)
      return
    }
    if (executed[changeSet.id] != null) return
    try {
      log.info("Executing $changeSet")
      changeSet.statements.forEach { db.exec(it) }
      repository.save(changeSet)
      tx.commit()
      log.info("Executed $changeSet")
    } catch (e: Exception) {
      tx.rollback()
      throw MigrationException(changeSet, e)
    }
  }
}

data class ChangeSet(
  override val id: String,
  val filePath: String,
  val context: String? = null,
): BaseEntity<String> {
  val separator: String? = ";"
  val sql = StringBuilder()

  operator fun plusAssign(line: String) {
    sql.append(line)
    if (line.isNotEmpty()) sql.append("\n")
  }

  val statements get() = (separator?.let { sql.split(it).asSequence() } ?: sequenceOf(sql.toString()))
    .map { it.trimEnd() }.filter { it.isNotEmpty() }

  val checksum: Long get() = statements.fold(0) { r, s -> r + s.hashCode() * 89 }
}

class ChangeSetRepository(db: DataSource): BaseCrudRepository<ChangeSet, String>(db, "db_changelog") {
  override val defaultOrder = "$orderAsc for update"
  override fun ChangeSet.persister() = toValuesSkipping(ChangeSet::separator, ChangeSet::sql)
}

class MigrationException(message: String, cause: Throwable? = null): RuntimeException(message, cause) {
  constructor(changeSet: ChangeSet, cause: Throwable? = null): this("Failed $changeSet", cause)
}

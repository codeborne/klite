package klite.jdbc

import klite.*
import klite.jdbc.ChangeSet.OnChange.*
import java.io.Reader
import java.lang.Thread.currentThread
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.collections.*

/** Work in progress replacement for Liquibase SQL format */
open class DBMigrator(
  private val db: DataSource,
  private val filePaths: List<String> = listOf(Config.optional("DB_MIGRATE", "db.sql")),
  private val contexts: Set<String> = Config.active,
  private val substitutions: MutableMap<String, String> = mutableMapOf(),
  private val dropAllOnFailure: Boolean = Config.isTest
): Extension {
  private val log = logger()
  private val commentRegex = "\\s*--.*".toRegex()
  private val substRegex = "\\\$\\{(\\w*)}".toRegex()
  private val repository = ChangeSetRepository(db)

  private val tx = Transaction(db)
  private var history: Map<String, ChangeSet> = emptyMap()

  override fun install(server: Server) = migrate()

  fun migrate() = try {
    doMigrate()
  } catch (e: MigrationException) {
    if (dropAllOnFailure) {
      log.error("Migration failed, dropping and retrying from scratch", e)
      dropAll()
      doMigrate()
    } else throw e
  }

  private fun doMigrate() = tx.attachToThread().use {
    history = readHistory()
    filePaths.forEach { migrateFile(it) }
  }

  fun dropAll(schema: String? = null) = tx.attachToThread().use {
    val schema = schema ?: db.withConnection { this.schema }
    log.warn("Dropping and recreating schema $schema")
    db.exec("drop schema $schema cascade")
    db.exec("create schema $schema")
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
      if (it.checksum == null) {
        log.info("Storing new checksum for ${changeSet.id}")
        return markRan(changeSet)
      }
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
      markRan(changeSet)
    } catch (e: Exception) {
      tx.rollback()
      when (changeSet.onFail) {
        SKIP -> log.warn("Skipping failed $changeSet: $e")
        MARK_RAN -> {
          log.warn("Marking as ran failed $changeSet: $e")
          markRan(changeSet)
        }
        else -> throw MigrationException(changeSet, e)
      }
    }
  }

  private fun markRan(changeSet: ChangeSet) {
    repository.save(changeSet)
    tx.commit()
  }
}

class MigrationException(message: String, cause: Throwable? = null): RuntimeException(message, cause) {
  constructor(changeSet: ChangeSet, cause: Throwable? = null): this("Failed $changeSet", cause)
  constructor(changeSet: ChangeSet, message: String): this("$changeSet\n - $message")
}

package klite.jdbc

import klite.info
import klite.logger
import java.io.Reader
import java.lang.Thread.currentThread
import java.sql.Connection

/** Work in progress replacement for Liquibase SQL format */
open class Migrator(
  private val conn: Connection,
  private val substitutions: Map<String, String> = emptyMap()
) {
  private val log = logger()
  private val commentRegex = "--.*".toRegex()
  private val substRegex = "\\\$\\{(\\w*)}".toRegex()

  fun migrate() {
    migrateFile("users.sql")
  }

  open fun readFile(path: String) = currentThread().contextClassLoader.getResourceAsStream(path) ?: throw MigrationException("$path not found in classpath")
  fun migrateFile(path: String) = readFile(path).reader().use { migrate(path, it) }

  private fun migrate(path: String, reader: Reader) {
    log.info("Migrating $path")
    val input = reader.buffered()
    val header = input.readLine()
    val expectedHeader = "--liquibase formatted sql"
    if (header != expectedHeader) throw MigrationException("Changeset file should start with `$expectedHeader` instead of `$header`")

    var changeSet = ChangeSet("", path)
    input.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
      if (line.startsWith("--changeset")) {
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
    try {
      conn.createStatement().use { stmt ->
        changeSet.statements.forEach { stmt.execute(it) }
      }
      conn.commit()
    } catch (e: Exception) {
      conn.rollback()
      throw MigrationException(changeSet, e)
    }
  }
}

data class ChangeSet(
  val id: String,
  val filePath: String,
  val separator: String = ";"
) {
  val sql = StringBuilder()

  operator fun plusAssign(line: String) {
    sql.append(line)
    if (line.isNotEmpty()) sql.append("\n")
  }

  val statements get() = sql.split(separator)
}

class MigrationException(message: String, cause: Throwable? = null): RuntimeException(message, cause) {
  constructor(changeSet: ChangeSet, cause: Throwable? = null): this("Failed $changeSet\n" + changeSet.sql, cause)
}

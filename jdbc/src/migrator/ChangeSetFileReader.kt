package klite.jdbc

import klite.info
import klite.logger
import java.io.FileNotFoundException
import java.io.Reader

/**
 * Reads DB changesets from sql files, similar to [Liquibase SQL format](https://docs.liquibase.com/concepts/changelogs/sql-format.html).
 * Default implementation reads files from classpath.
 *
 * Lines starting with the following are treated specially:
 * * `--include path/file.sql` includes another sql file
 * * `--substitute a=b` can be used to substitute ${a} on subsequent lines with b
 * * `--changeset some-id onChange:SKIP` - changeset declarations themselves, starting with its unique id and optional attributes
 */
open class ChangeSetFileReader(
  private vararg val filePaths: String,
  private val substitutions: MutableMap<String, String> = mutableMapOf(),
): Sequence<ChangeSet> {
  private val log = logger()
  private val commentRegex = "\\s*--.*".toRegex()
  private val substRegex = "\\\$\\{(\\w*)}".toRegex()
  private val whitespace = "\\s+".toRegex()

  override fun iterator() = readAll().iterator()
  private fun readAll(): Sequence<ChangeSet> = sequence { filePaths.forEach { readFile(it) } }

  open fun resolveFile(path: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(path) ?: throw FileNotFoundException("$path not found in classpath")

  private suspend fun SequenceScope<ChangeSet>.readFile(path: String) = resolveFile(path).reader().use { read(it, path) }

  private suspend fun SequenceScope<ChangeSet>.read(reader: Reader, path: String) {
    log.info("Reading $path")
    var changeSet = ChangeSet("")
    reader.buffered().lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.forEach { line ->
      if (line.startsWith("--include")) {
        yield(changeSet)
        readFile(line.substringAfter("--include").trim())
      }
      else if (line.startsWith("--substitute")) {
        yield(changeSet)
        val (key, value) = line.substringAfter("--substitute").trim().split('=', limit = 2)
        substitutions[key] = value
      }
      else if (line.startsWith("--changeset")) {
        yield(changeSet)
        val parts = line.split(whitespace)
        val args = mapOf(ChangeSet::id.name to parts[1], ChangeSet::filePath.name to path) +
          parts.drop(2).associate { it.split(":", limit = 2).let { it[0] to it[1] } }
        changeSet = args.fromValues()
      }
      else changeSet.addLine(line.replace(commentRegex, "").substitute())
    }
    yield(changeSet)
  }

  private fun String.substitute() = substRegex.replace(this) { substitutions[it.groupValues[1]] ?: error("Unknown substitution ${it.value}") }
}

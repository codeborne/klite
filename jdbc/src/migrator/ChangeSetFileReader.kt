package klite.jdbc

import klite.info
import klite.jdbc.ChangeSetFileReader.keywords.*
import klite.logger
import java.io.FileNotFoundException
import java.io.Reader
import kotlin.reflect.full.primaryConstructor

/**
 * Reads DB changesets from sql files, similar to [Liquibase SQL format](https://docs.liquibase.com/concepts/changelogs/sql-format.html).
 * Default implementation reads files from classpath.
 *
 * Lines starting with the following are treated specially:
 * * `--include path/file.sql` includes another sql file from classpath
 * * `--substitute a=b` can be used to substitute ${a} on subsequent lines with b
 * * `--changeset some-id onChange:SKIP` - changeset declarations themselves, starting with its unique id and optional attributes
 */
open class ChangeSetFileReader(
  private vararg val filePaths: String,
  private val substitutions: MutableMap<String, String> = mutableMapOf(),
): Sequence<ChangeSet> {
  private val log = logger()
  private val changeSetParams = ChangeSet::class.primaryConstructor!!.parameters.map { it.name }.toSet()
  private val commentRegex = "\\s*--.*".toRegex()
  private val substRegex = "\\\$\\{(\\w*)}".toRegex()
  private val whitespace = "\\s+".toRegex()

  override fun iterator() = readAll().iterator()
  private fun readAll(): Sequence<ChangeSet> = sequence { filePaths.forEach { readFile(it) } }

  open fun resolveFile(path: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(path) ?: throw FileNotFoundException("$path not found in classpath")

  private suspend fun SequenceScope<ChangeSet>.readFile(path: String) = resolveFile(path).reader().use { read(it, path) }

  enum class keywords { include, substitute, changeset }

  private suspend fun SequenceScope<ChangeSet>.read(reader: Reader, path: String) {
    log.info("Reading $path")
    var changeSet = ChangeSet("")

    reader.buffered().lineSequence().map { it.trimEnd() }.filter { it.isNotEmpty() }.forEach { line ->
      val keyword = if (line.startsWith("--")) keywords.values().find { line.startsWith("--$it") } else null
      val rest = if (keyword != null) {
        if (changeSet.isNotEmpty()) yield(changeSet.finish())
        changeSet = ChangeSet("")
        line.substring(keyword.name.length + 3).trim()
      } else line

      when (keyword) {
        include -> readFile(rest)
        substitute -> {
          val (key, value) = rest.split('=')
          substitutions[key] = value
        }
        changeset -> {
          val parts = rest.split(whitespace)
          changeSet = (mapOf(ChangeSet::id.name to parts[0], ChangeSet::filePath.name to path) +
            parts.drop(1).associate { it.split(":", limit = 2).let { (k, v) ->
              require(k in changeSetParams) { "Unknown $changeset param $k, supported: $changeSetParams" }
              k to v
            }}).fromValues()
        }
        else -> changeSet.addLine(line.replace(commentRegex, "").substitute())
      }
    }
    if (changeSet.isNotEmpty()) yield(changeSet.finish())
  }

  private fun String.substitute() = substRegex.replace(this) { substitutions[it.groupValues[1]] ?: error("Unknown substitution ${it.value}") }
}

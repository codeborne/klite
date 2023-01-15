package klite.jdbc

import klite.info
import klite.logger
import java.io.FileNotFoundException
import java.io.Reader

open class ChangeSetFileReader(
  private vararg val filePaths: String,
  private val substitutions: MutableMap<String, String> = mutableMapOf(),
): Sequence<ChangeSet> {
  private val log = logger()
  private val commentRegex = "\\s*--.*".toRegex()
  private val substRegex = "\\\$\\{(\\w*)}".toRegex()

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
        val parts = line.split("\\s+".toRegex())
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

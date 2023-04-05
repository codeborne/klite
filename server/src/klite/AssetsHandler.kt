package klite

import klite.StatusCode.Companion.OK
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import kotlin.io.path.*

open class AssetsHandler(
  val path: Path,
  val indexFile: String = "index.html",
  val useIndexForUnknownPaths: Boolean = false,
  val additionalHeaders: Map<String, String> = mapOf("Cache-Control" to "max-age=604800"),
  val indexHeaders: Map<String, String> = mapOf("Cache-Control" to "max-age=0, must-revalidate"),
  val useChunkedResponseForFilesLargerThan: Long = 30 * (1L shl 20),
  val headerModifier: HttpExchange.() -> Unit = {}
): Handler {
  private val logger = logger()

  init {
    if (!path.isDirectory()) logger.warn("Assets path ${path.toAbsolutePath()} is not a readable directory")
  }

  override suspend fun invoke(exchange: HttpExchange) {
    try {
      var file = path / exchange.path.substring(1)
      if (!file.startsWith(path)) throw ForbiddenException(exchange.path)
      if (file.isDirectory()) file /= indexFile
      if (!file.exists()) {
        if (useIndexForUnknownPaths && !file.name.contains(".")) file = path / indexFile
        else throw NotFoundException(exchange.path)
      }
      exchange.send(file)
    } catch (e: FileNotFoundException) {
      throw NotFoundException(e.message, e)
    }
  }

  protected open fun HttpExchange.send(file: Path) {
    checkLastModified(file.getLastModifiedTime().toInstant())
    responseHeaders += additionalHeaders
    if (file.endsWith(indexFile)) responseHeaders += indexHeaders
    var contentType = MimeTypes.typeFor(file)
    if (contentType == null) {
      logger.warn("Cannot detect content-type for $file")
      contentType = MimeTypes.unknown
    }

    headerModifier()

    val gzFile = Path.of("$file.gz")
    val fileToSend = if (header("Accept-Encoding")?.contains("gzip") == true && gzFile.exists()) {
      header("Content-Encoding", "gzip")
      gzFile
    } else file

    val out = startResponse(OK, fileToSend.fileSize().takeIf { it <= useChunkedResponseForFilesLargerThan }, contentType)
    fileToSend.inputStream(READ).use { it.transferTo(out) }
  }
}

fun HttpExchange.checkLastModified(at: Instant) {
  if (lastModified(at) == header("If-Modified-Since")) throw NotModifiedException()
}

fun HttpExchange.lastModified(at: Instant): String = RFC_1123_DATE_TIME.format(at.atOffset(UTC)).also {
  header("Last-Modified", it)
}

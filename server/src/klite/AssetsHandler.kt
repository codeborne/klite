package klite

import klite.StatusCode.Companion.OK
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import kotlin.io.path.*
import kotlin.time.Duration.Companion.days

open class AssetsHandler(
  val path: Path,
  val indexFile: String = "index.html",
  val useIndexForUnknownPaths: Boolean = false,
  val additionalHeaders: Map<String, String> = mapOf("Cache-Control" to "max-age=${7.days.inWholeSeconds}"),
  val indexHeaders: Map<String, String> = mapOf("Cache-Control" to "max-age=0, must-revalidate"),
  val useChunkedResponseForFilesLargerThan: Long = 30 * (1L shl 20),
  val headerModifier: HttpExchange.(file: Path) -> Unit = {}
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
      send(exchange, file)
    } catch (e: FileNotFoundException) {
      throw NotFoundException(e.message, e)
    }
  }

  protected open fun send(e: HttpExchange, file: Path) = e.apply {
    checkLastModified(file.getLastModifiedTime().toInstant())
    responseHeaders += additionalHeaders
    if (file.endsWith(indexFile)) responseHeaders += indexHeaders
    var contentType = MimeTypes.typeFor(file)
    if (contentType == null) {
      logger.warn("Cannot detect content-type for $file")
      contentType = MimeTypes.unknown
    }

    headerModifier(file)

    val gzFile = Path.of("$file.gz")
    val fileToSend = if (header("Accept-Encoding")?.contains("gzip") == true && gzFile.exists()) {
      header("Content-Encoding", "gzip")
      gzFile
    } else file

    val out = startResponse(OK, fileToSend.fileSize().takeIf { it <= useChunkedResponseForFilesLargerThan }, contentType)
    fileToSend.inputStream(READ).use { it.transferTo(out) }
  }
}

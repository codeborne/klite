package klite

import klite.StatusCode.Companion.NotModified
import klite.StatusCode.Companion.OK
import java.io.IOException
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import kotlin.io.path.*
import kotlin.text.Charsets.UTF_8

class AssetsHandler(
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
    } catch (e: IOException) {
      throw NotFoundException(e.message)
    }
  }

  private fun HttpExchange.send(file: Path) {
    val lastModified = RFC_1123_DATE_TIME.format(file.getLastModifiedTime().toInstant().atOffset(UTC))
    header("Last-Modified", lastModified)
    if (lastModified == header("If-Modified-Since")) return send(NotModified)

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

object MimeTypes {
  var unknown = "application/octet-stream"
  val wwwForm = "application/x-www-form-urlencoded"
  var textCharset = UTF_8
  val typesByExtension = mutableMapOf(
    "html" to "text/html",
    "txt" to "text/plain",
    "xml" to "text/xml",
    "xsd" to "text/xml",
    "js" to "text/javascript",
    "mjs" to "text/javascript",
    "json" to "application/json",
    "css" to "text/css",
    "csv" to "text/csv",
    "ico" to "image/vnd.microsoft.icon",
    "png" to "image/png",
    "gif" to "image/gif",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "webp" to "image/webp",
    "svg" to "image/svg+xml",
    "mp4" to "video/mp4",
    "ogv" to "video/ogv",
    "oga" to "audio/oga",
    "mp3" to "audio/mpeg",
    "ttf" to "font/ttf",
    "otf" to "font/otf",
    "woff" to "font/woff",
    "woff2" to "font/woff2",
    "pdf" to "application/pdf",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "zip" to "application/zip",
    "jar" to "application/java-archive",
    "gz" to "application/gzip",
    "ics" to "text/calendar",
    "asice" to "application/vnd.etsi.asic-e+zip"
  )

  fun typeFor(file: Path) = typesByExtension[file.extension]
  fun typeFor(fileName: String) = typeFor(Path.of(fileName))

  fun isText(contentType: String) = contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml")
  fun withCharset(contentType: String) = if (!contentType.contains("charset") && isText(contentType)) "$contentType; charset=$textCharset" else contentType
}

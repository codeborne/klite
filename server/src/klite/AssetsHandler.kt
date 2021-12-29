package klite

import java.io.IOException
import java.lang.System.Logger.Level.WARNING
import java.nio.charset.Charset
import java.nio.file.Path
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import kotlin.io.path.*
import kotlin.text.Charsets.UTF_8

class AssetsHandler(
  val path: Path,
  val indexFile: String = "index.html",
  val useIndexForUnknownPaths: Boolean = false,
  val additionalHeaders: Map<String, String> = mapOf("Cache-Control" to "max-age=86400"),
  val mimeTypes: MimeTypes = MimeTypes(),
  val textCharset: Charset = UTF_8
): Handler {
  private val logger = logger()

  init {
    if (!path.isDirectory()) logger.log(WARNING, "Assets path ${path.toAbsolutePath()} is not a readable directory")
  }

  override suspend fun invoke(exchange: HttpExchange) {
    // TODO AsynchronousFileChannel.open(path.resolve(exchange.requestPath), READ).read().await()
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
    if (lastModified == header("If-Modified-Since")) return send(StatusCode.NotModified)
    header("Last-Modified", lastModified)
    additionalHeaders.forEach { (k, v) -> header(k, v) }
    var contentType: String? = mimeTypes.typeFor(file)
    if (contentType == null) logger.warn("Cannot detect content-type for $file")
    else if (mimeTypes.isText(contentType)) contentType += "; charset=$textCharset"
    send(StatusCode.OK, file.readBytes(), contentType)
  }
}

class MimeTypes(moreTypesByFileExtension: Map<String, String> = emptyMap()) {
  private val typesByExtension = mapOf(
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
    "gz" to "application/gzip",
    "ics" to "text/calendar"
  ) + moreTypesByFileExtension

  fun typeFor(file: Path) = typesByExtension[file.extension]
  fun isText(contentType: String) = contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml")
}

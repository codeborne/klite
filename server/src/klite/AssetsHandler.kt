@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package klite

import sun.net.www.MimeTable
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
  val cacheControl: String = "max-age=86400",
  val textCharset: Charset = UTF_8
): Handler {
  private val mimeTypes = MimeTable.getDefaultTable()
  private val logger = System.getLogger(javaClass.name)

  init {
    if (!path.isDirectory()) logger.log(WARNING, "Assets path ${path.toAbsolutePath()} is not a readable directory")
  }

  override suspend fun invoke(exchange: HttpExchange) {
    // TODO AsynchronousFileChannel.open(path.resolve(exchange.requestPath), READ).read().await()
    try {
      var file = path / exchange.path.substring(1)
      if (!file.startsWith(path)) return exchange.send(403, exchange.path)
      if (file.isDirectory()) file /= indexFile
      if (!file.exists()) throw NotFoundException(exchange.path)
      exchange.send(file)
    } catch (e: IOException) {
      throw NotFoundException(e.message)
    }
  }

  private fun HttpExchange.send(file: Path) {
    val lastModified = RFC_1123_DATE_TIME.format(file.getLastModifiedTime().toInstant().atOffset(UTC))
    if (lastModified == header("If-Modified-Since")) return send(304)
    header("Last-Modified", lastModified)
    header("Cache-Control", cacheControl)
    var contentType = mimeTypes.getContentTypeFor(file.name)
    if (contentType.startsWith("text/")) contentType += "; charset=$textCharset"
    send(200, file.readBytes(), contentType)
  }
}

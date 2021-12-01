@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package server

import sun.net.www.MimeTable
import java.io.IOException
import java.nio.file.Path
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import kotlin.io.path.div
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import kotlin.io.path.readBytes

class AssetsHandler(val path: Path, val indexFile: String = "index.html", val cacheControl: String = "max-age=86400"): Handler {
  private val mimeTypes = MimeTable.getDefaultTable()

  override suspend fun invoke(exchange: HttpExchange) {
    // TODO AsynchronousFileChannel.open(path.resolve(exchange.requestPath), READ).read().await()
    try {
      val file = path / exchange.path.substring(1)
      if (!file.startsWith(path)) return exchange.send(403, file)
      val lastModified = RFC_1123_DATE_TIME.format(file.getLastModifiedTime().toInstant().atOffset(UTC))
      if (lastModified == exchange.header("if-modified-since")) return exchange.send(304, null)
      exchange.responseHeaders["Last-Modified"] = lastModified
      exchange.responseHeaders["Cache-Control"] = cacheControl
      exchange.send(200, file.readBytes(), mimeTypes.getContentTypeFor(file.name))
    } catch (e: IOException) {
      exchange.send(404, e.message)
    }
  }
}

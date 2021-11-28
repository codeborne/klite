@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import sun.net.www.MimeTable
import java.nio.file.Path
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import kotlin.io.path.div
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import kotlin.io.path.readBytes

class AssetsHandler(val path: Path, val indexFile: String = "index.html", val cacheControl: String = "max-age=86400"): HttpHandler {
  val mimeTypes = MimeTable.getDefaultTable()

  override fun handle(exchange: HttpExchange) {
    // TODO AsynchronousFileChannel.open(path.resolve(exchange.requestPath), READ).read().await()
    try {
      val file = path / exchange.requestPath.substring(1)
      if (!file.startsWith(path)) return exchange.send(403, file)
      val lastModified = RFC_1123_DATE_TIME.format(file.getLastModifiedTime().toInstant().atOffset(UTC))
      if (lastModified == exchange.requestHeaders.getFirst("if-modified-since")) return exchange.send(304, null)
      exchange.responseHeaders["last-modified"] = lastModified
      exchange.responseHeaders["cache-control"] = cacheControl
      exchange.responseHeaders["content-type"] = mimeTypes.getContentTypeFor(file.name)
      exchange.send(200, file.readBytes())
    } catch (e: Exception) {
      exchange.send(404, e.message)
    } finally {
      exchange.close()
    }
  }
}
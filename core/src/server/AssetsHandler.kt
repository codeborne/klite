@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package server

import sun.net.www.MimeTable
import java.io.IOException
import java.lang.System.Logger.Level.WARNING
import java.nio.file.Path
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import kotlin.io.path.*

class AssetsHandler(
  val path: Path,
  val indexFile: String = "index.html",
  val cacheControl: String = "max-age=86400"
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
      if (!file.exists()) return exchange.send(404, exchange.path)
      send(file, exchange)
    } catch (e: IOException) {
      exchange.send(404, e.message)
    }
  }

  private fun send(file: Path, exchange: HttpExchange) {
    val lastModified = RFC_1123_DATE_TIME.format(file.getLastModifiedTime().toInstant().atOffset(UTC))
    if (lastModified == exchange.header("If-Modified-Since")) return exchange.send(304, null)
    exchange.header("Last-Modified", lastModified)
    exchange.header("Cache-Control", cacheControl)
    exchange.send(200, file.readBytes(), mimeTypes.getContentTypeFor(file.name))
  }
}

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import sun.net.www.MimeTable
import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.nio.file.Path
import java.text.DateFormat
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.io.path.div
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import kotlin.io.path.readBytes

class Server(
  val port: Int = 8080,
  val numWorkers: Int = getRuntime().availableProcessors(),
  val defaultContentType: String = "text/plain",
  val defaultFilters: List<AsyncFilter> = listOf(RequestLogger())
) {
  private val log = Logger.getLogger(javaClass.name)
  val workerPool = Executors.newFixedThreadPool(numWorkers)
  val requestScope = CoroutineScope(SupervisorJob() + workerPool.asCoroutineDispatcher())
  private val http = HttpServer.create(InetSocketAddress(port), 0)

  fun start() = http.start().also {
    log.info("Listening on $port")
    getRuntime().addShutdownHook(thread(start = false) { stop() })
  }

  fun stop(delaySec: Int = 3) {
    log.info("Stopping gracefully")
    http.stop(delaySec)
  }

  fun route(prefix: String, handler: Handler) = http.createContext(prefix) { exchange ->
    requestScope.launch {
      runHandler(exchange, handler)
    }
  }.apply {
    log.info("Route: $prefix")
    filters.addAll(defaultFilters)
  }

  fun assets(prefix: String, path: Path, cacheControl: String = "max-age=86400") {
    val mimeTypes = MimeTable.getDefaultTable()
    val httpDate = DateTimeFormatter.RFC_1123_DATE_TIME
    http.createContext(prefix) { exchange ->
      requestScope.launch(Dispatchers.IO) {
        // TODO AsynchronousFileChannel.open(path.resolve(exchange.requestPath), READ).read().await()
        try {
          val file = path / exchange.requestPath.substring(1)
          if (!file.startsWith(path)) return@launch exchange.send(403, file)
          exchange.responseHeaders["content-type"] = mimeTypes.getContentTypeFor(file.name)
          exchange.responseHeaders["last-modified"] = httpDate.format(file.getLastModifiedTime().toInstant().atOffset(UTC))
          exchange.responseHeaders["cache-control"] = cacheControl
          exchange.send(200, file.readBytes())
        } catch (e: Exception) {
          exchange.send(404, e.message)
        } finally {
          exchange.close()
        }
      }
    }
  }

  private suspend fun runHandler(exchange: HttpExchange, handler: Handler) {
    exchange.responseHeaders["content-type"] = listOf(defaultContentType)
    try {
      val result = handler()
      exchange.forEachFilter { after(exchange, null) }
      exchange.send(200, result)
    } catch (e: Throwable) {
      exchange.forEachFilter { after(exchange, e) }
      log.log(Level.SEVERE, "Unhandled throwable", e)
      exchange.send(500, e)
    } finally {
      exchange.close()
    }
  }

  private fun HttpExchange.forEachFilter(callback: AsyncFilter.() -> Unit) = httpContext.filters.forEach {
    (it as? AsyncFilter)?.callback()
  }
}

typealias Handler = suspend () -> Any?

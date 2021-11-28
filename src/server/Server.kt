package server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread

class Server(
  val port: Int = 8080,
  val numWorkers: Int = getRuntime().availableProcessors(),
  val defaultContentType: String = "text/plain"
) {
  private val log = Logger.getLogger(javaClass.name)
  val workerPool = Executors.newFixedThreadPool(numWorkers)
  val requestScope = CoroutineScope(SupervisorJob() + workerPool.asCoroutineDispatcher())
  private val http = HttpServer.create(InetSocketAddress(port), 0)

  fun start() {
    http.start()
    log.info("Listening on $port")
    getRuntime().addShutdownHook(thread(start = false) { stop() })
  }

  fun stop(delaySec: Int = 5) {
    log.info("Stopping gracefully")
    http.stop(delaySec)
  }

  fun route(path: String, handler: Handler) {
    log.info("Route: $path")
    http.createContext(path) { exchange ->
      requestScope.launch {
        exchange.responseHeaders["Content-Type"] = listOf(defaultContentType)
        try {
          val result = handler().toString()
          exchange.send(200, result)
        } catch (e: Throwable) {
          log.log(Level.SEVERE, "Unhandled throwable", e)
          exchange.send(500, e.toString())
        } finally {
          exchange.close()
        }
      }
    }
  }

  private fun HttpExchange.send(resCode: Int, content: String) {
    val bytes = content.toByteArray()
    sendResponseHeaders(resCode, bytes.size.toLong())
    responseBody.write(bytes)
  }
}

typealias Handler = suspend () -> Any?

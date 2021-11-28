package server

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.logging.Logger
import kotlin.concurrent.thread

class Server(
  val port: Int = 8080,
  val numWorkers: Int = getRuntime().availableProcessors()
) {
  var log = Logger.getLogger(javaClass.name)
  val dispatcher = Executors.newFixedThreadPool(numWorkers).asCoroutineDispatcher()
  val requestScope = CoroutineScope(SupervisorJob() + dispatcher)
  private val http = HttpServer.create(InetSocketAddress(port), 0)

  fun start() {
    http.executor = null // receive requests on the main thread
    http.createContext("/") { exchange ->
      requestScope.launch {
        delay(100)
        val response = "Hello World"
        exchange.responseHeaders["Content-Type"] = listOf("text/plain")
        exchange.sendResponseHeaders(200, response.length.toLong())
        exchange.responseBody.write(response.toByteArray())
        exchange.close()
      }
    }
    http.start()
    log.info("Listening on $port")
    getRuntime().addShutdownHook(thread(start = false) { stop() })
  }

  fun stop(delaySec: Int = 5) {
    log.info("Stopping gracefully")
    http.stop(delaySec)
  }
}

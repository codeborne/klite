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
      exchange.responseHeaders["Content-Type"] = listOf(defaultContentType)
      try {
        val result = handler().toString()
        exchange.send(200, result)
      } catch (e: Throwable) {
        log.log(Level.SEVERE, "Unhandled throwable", e)
        exchange.send(500, e)
      } finally {
        exchange.httpContext.filters.forEach { (it as? AsyncFilter)?.after(exchange) }
        exchange.close()
      }
    }
  }.apply {
    log.info("Route: $prefix")
    filters.addAll(defaultFilters)
  }
}

typealias Handler = suspend () -> Any?

abstract class AsyncFilter: com.sun.net.httpserver.Filter() {
  override fun description() = javaClass.simpleName

  open fun before(exchange: HttpExchange) {}
  open fun after(exchange: HttpExchange) {}

  override fun doFilter(exchange: HttpExchange, chain: Chain) {
    before(exchange)
    chain.doFilter(exchange)
  }
}

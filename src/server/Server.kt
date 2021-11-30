package server

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import server.RequestMethod.GET
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
  val defaultFilters: List<AsyncFilter> = listOf(RequestLogger()),
  val pathParamRegexer: PathParamRegexer = PathParamRegexer()
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

  fun context(prefix: String, block: Router.() -> Unit = {}) = Router(prefix, pathParamRegexer).apply {
    val context = http.createContext(prefix) { exchange ->
      requestScope.launch {
        process(exchange, route(exchange))
      }
    }
    context.filters.addAll(defaultFilters)
    block()
  }

  fun assets(prefix: String, handler: AssetsHandler) = http.createContext(prefix) { exchange ->
    requestScope.launch(Dispatchers.IO) {
      process(exchange, handler.takeIf { exchange.requestMethod == GET.name })
    }
  }

  private suspend fun process(exchange: HttpExchange, handler: Handler?) {
    try {
      val result = handler?.invoke(exchange) ?: return exchange.send(404, exchange.requestPath)
      exchange.forEachFilter { after(exchange, null) }
      exchange.send(200, result, defaultContentType)
    } catch (e: Throwable) {
      exchange.forEachFilter { after(exchange, e) }
      if (e is StatusCodeException) exchange.send(e.statusCode, e.message)
      else {
        log.log(Level.SEVERE, "Unhandled throwable", e)
        exchange.send(500, e)
      }
    } finally {
      exchange.close()
    }
  }

  private fun HttpExchange.forEachFilter(callback: AsyncFilter.() -> Unit) = httpContext.filters.forEach {
    (it as? AsyncFilter)?.callback()
  }
}

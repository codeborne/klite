package server

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import server.RequestMethod.GET
import java.lang.Runtime.getRuntime
import java.lang.System.Logger.Level.ERROR
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class Server(
  val port: Int = 8080,
  val numWorkers: Int = getRuntime().availableProcessors(),
  val defaultContentType: String = "text/plain",
  val globalDecorators: List<Decorator> = listOf(RequestLogger()),
  val pathParamRegexer: PathParamRegexer = PathParamRegexer(),
  val launchContext: HttpExchange.() -> CoroutineContext = { EmptyCoroutineContext }
) {
  private val logger = System.getLogger(javaClass.name)
  val workerPool = Executors.newFixedThreadPool(numWorkers)
  val requestScope = CoroutineScope(SupervisorJob() + workerPool.asCoroutineDispatcher())
  private val http = HttpServer.create(InetSocketAddress(port), 0)

  fun start(stopOnShutdown: Boolean = true) = http.start().also {
    logger.info("Listening on $port")
    if (stopOnShutdown) getRuntime().addShutdownHook(thread(start = false) { stop() })
  }

  fun stop(delaySec: Int = 3) {
    logger.info("Stopping gracefully")
    http.stop(delaySec)
  }

  fun context(prefix: String, block: Router.() -> Unit = {}) = Router(prefix, pathParamRegexer, globalDecorators).apply {
    http.createContext(prefix) { ex ->
      val exchange = HttpExchange(ex)
      requestScope.launch(launchContext(exchange)) {
        handle(exchange, route(exchange))
      }
    }
    block()
  }

  fun assets(prefix: String, handler: AssetsHandler) {
    http.createContext(prefix) { ex ->
      requestScope.launch(Dispatchers.IO) {
        val exchange = HttpExchange(ex)
        handle(exchange, handler.takeIf { exchange.method == GET })
      }
    }
  }

  private suspend fun handle(exchange: HttpExchange, handler: Handler?) {
    try {
      val result = handler?.invoke(exchange) ?: return exchange.send(404, exchange.path)
      exchange.send(200, result, defaultContentType)
    } catch (e: Throwable) {
      if (e is StatusCodeException) exchange.send(e.statusCode, e.message)
      else {
        logger.log(ERROR, "Unhandled throwable", e)
        exchange.send(500, e)
      }
    } finally {
      exchange.close()
    }
  }
}

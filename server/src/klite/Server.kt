package klite

import com.sun.net.httpserver.HttpServer
import klite.RequestMethod.GET
import kotlinx.coroutines.*
import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class Server(
  val port: Int = System.getenv("PORT")?.toInt() ?: 8080,
  val numWorkers: Int = getRuntime().availableProcessors(),
  val registry: MutableRegistry = DependencyInjectingRegistry().apply {
    register<RequestLogger>()
    register<TextBodyRenderer>()
    register<TextBodyParser>()
    register<FormUrlEncodedParser>()
  },
  val globalDecorators: List<Decorator> = registry.requireAllDecorators(),
  val errorHandler: ErrorHandler = registry.require(),
  val bodyRenderers: List<BodyRenderer> = registry.requireAll(),
  val bodyParsers: List<BodyParser> = registry.requireAll(),
  val pathParamRegexer: PathParamRegexer = registry.require(),
  val httpExchangeImpl: KClass<out HttpExchange> = XForwardedHttpExchange::class,
): Registry by registry {
  private val logger = logger()
  val workerPool = Executors.newFixedThreadPool(numWorkers)
  val requestScope = CoroutineScope(SupervisorJob() + workerPool.asCoroutineDispatcher())
  private val http = HttpServer.create()

  fun start(stopOnShutdown: Boolean = true) {
    http.bind(InetSocketAddress(port), 0)
    http.start()
    logger.info("Listening on $port")
    if (stopOnShutdown) getRuntime().addShutdownHook(thread(start = false) { stop() })
  }

  fun stop(delaySec: Int = 3) {
    logger.info("Stopping gracefully")
    http.stop(delaySec)
  }

  fun context(prefix: String, block: Router.() -> Unit = {}) = Router(prefix, pathParamRegexer, globalDecorators, bodyRenderers, bodyParsers).apply {
    http.createContext(prefix) { ex ->
      requestScope.launch {
        httpExchangeImpl.primaryConstructor!!.call(ex, bodyRenderers, bodyParsers).let { handle(it, route(it)) }
      }
    }
    block()
  }

  fun assets(prefix: String, handler: AssetsHandler) {
    http.createContext(prefix) { ex ->
      requestScope.launch(Dispatchers.IO) {
        val exchange = HttpExchange(ex, bodyRenderers, emptyList())
        handle(exchange, handler.takeIf { exchange.method == GET })
      }
    }
  }

  private suspend fun handle(exchange: HttpExchange, handler: Handler?) {
    try {
      handler ?: return exchange.send(StatusCode.NotFound, exchange.path)
      val result = handler.invoke(exchange).takeIf { it != Unit }
      if (!exchange.isResponseStarted)
        exchange.render(if (result == null) StatusCode.NoContent else StatusCode.OK, result)
    } catch (e: Exception) {
      errorHandler(exchange, e)
    } finally {
      exchange.close()
    }
  }
}

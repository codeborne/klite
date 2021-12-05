package klite

import com.sun.net.httpserver.HttpServer
import klite.RequestMethod.GET
import kotlinx.coroutines.*
import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class Server(
  val port: Int = System.getenv("PORT")?.toInt() ?: 8080,
  // TODO: service registry
  val numWorkers: Int = getRuntime().availableProcessors(),
  val registry: MutableRegistry = DependencyInjectingRegistry().apply {
    register(RequestLogger().toDecorator())
    register<TextBodyRenderer>()
    register<TextBodyParser>()
    register<FormUrlEncodedParser>()
  },
  val globalDecorators: List<Decorator> = registry.requireAll(),
  val exceptionHandler: ExceptionHandler = registry.require<DefaultExceptionHandler>(),
  val bodyRenderers: List<BodyRenderer> = registry.requireAll(),
  val bodyParsers: List<BodyParser> = registry.requireAll(),
  val pathParamRegexer: PathParamRegexer = registry.require(),
): Registry by registry {
  private val logger = logger()
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
      requestScope.launch {
        HttpExchange(ex, bodyRenderers, bodyParsers).let { handle(it, route(it)) }
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
      exceptionHandler(exchange, e)
    } finally {
      exchange.close()
    }
  }
}

package klite

import com.sun.net.httpserver.HttpServer
import klite.RequestMethod.GET
import klite.StatusCode.Companion.NoContent
import klite.StatusCode.Companion.OK
import kotlinx.coroutines.*
import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

class Server(
  val listen: InetSocketAddress = InetSocketAddress(Config.optional("PORT")?.toInt() ?: 8080),
  val numWorkers: Int = Config.optional("NUM_WORKERS")?.toInt() ?: getRuntime().availableProcessors(),
  override val registry: MutableRegistry = DependencyInjectingRegistry().apply {
    register<RequestLogger>()
    register<TextBodyRenderer>()
    register<TextBodyParser>()
    register<FormUrlEncodedParser>()
  },
  val errors: ErrorHandler = registry.require(),
  decorators: List<Decorator> = registry.requireAllDecorators(),
  val sessionStore: SessionStore? = registry.optional(),
  val notFoundHandler: Handler = decorators.wrap { throw NotFoundException(path) },
  override val pathParamRegexer: PathParamRegexer = registry.require(),
  val httpExchangeCreator: KFunction<HttpExchange> = XForwardedHttpExchange::class.primaryConstructor!!,
): RouterConfig(decorators, registry.requireAll(), registry.requireAll()), MutableRegistry by registry {
  private val logger = logger()
  private val http = HttpServer.create()
  val workerPool = Executors.newFixedThreadPool(numWorkers)
  val requestScope = CoroutineScope(SupervisorJob() + workerPool.asCoroutineDispatcher())

  fun start(gracefulStopDelaySec: Int = 3) {
    logger.info("Listening on $listen")
    http.bind(listen, 0)
    http.start()
    if (gracefulStopDelaySec >= 0) getRuntime().addShutdownHook(thread(start = false) { stop(gracefulStopDelaySec) })
  }

  private val onStopHandlers = mutableListOf<Runnable>()
  fun onStop(handler: Runnable) { onStopHandlers += handler }

  fun stop(delaySec: Int = 1) {
    logger.info("Stopping gracefully")
    http.stop(delaySec)
    onStopHandlers.forEach { it.run() }
  }

  inline fun <reified E: Extension> use() = require<E>().also { it.install(this) }
  fun use(extension: Extension) = extension.also {
    register(it)
    it.install(this)
  }

  /** Adds a new router context. When handing a request, the longest matching router context is chosen. */
  fun context(prefix: String, block: Router.() -> Unit = {}) =
    Router(prefix, registry, pathParamRegexer, decorators, renderers, parsers).also { router ->
      addContext(prefix, router) {
        runHandler(this, router.route(this))
      }
      router.block()
    }

  fun assets(prefix: String, handler: AssetsHandler) {
    val route = Route(GET, prefix.toRegex(), decorators.wrap(handler))
    addContext(prefix, this, Dispatchers.IO) { runHandler(this, route.takeIf { method == GET }) }
  }

  private fun addContext(prefix: String, config: RouterConfig, coroutineContext: CoroutineContext = EmptyCoroutineContext, handler: Handler) {
    http.createContext(prefix) { ex ->
      requestScope.launch(coroutineContext) {
        httpExchangeCreator.call(ex, config, sessionStore).handler()
      }
    }
  }

  private suspend fun runHandler(exchange: HttpExchange, route: Route?) {
    try {
      if (route != null) exchange.route = route
      val result = (route?.handler ?: notFoundHandler).invoke(exchange)
      if (!exchange.isResponseStarted) {
        if (result == Unit) exchange.send(NoContent)
        else exchange.render(OK, result)
      }
    } catch (e: Exception) {
      errors.handle(exchange, e)
    } finally {
      exchange.close()
    }
  }
}

interface Extension {
  fun install(server: Server)
}

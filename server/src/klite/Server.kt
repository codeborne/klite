package klite

import com.sun.net.httpserver.HttpServer
import klite.RequestMethod.GET
import klite.RequestMethod.HEAD
import klite.StatusCode.Companion.NoContent
import klite.StatusCode.Companion.NotFound
import klite.StatusCode.Companion.OK
import kotlinx.coroutines.*
import java.lang.Runtime.getRuntime
import java.lang.Thread.currentThread
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

class Server(
  val listen: InetSocketAddress = InetSocketAddress(Config.optional("PORT")?.toInt() ?: 8080),
  val workerPool: ExecutorService = Executors.newFixedThreadPool(Config.optional("NUM_WORKERS")?.toInt() ?: getRuntime().availableProcessors()),
  override val registry: MutableRegistry = DependencyInjectingRegistry().apply {
    register<RequestLogger>()
    register<TextBodyRenderer>()
    register<TextBodyParser>()
    register<FormUrlEncodedParser>()
    register<MultipartFormDataParser>()
  },
  private val requestIdGenerator: RequestIdGenerator = registry.require(),
  val errors: ErrorHandler = registry.require(),
  decorators: List<Decorator> = registry.requireAllDecorators(),
  private val sessionStore: SessionStore? = registry.optional(),
  val notFoundHandler: Handler = decorators.wrap { ErrorResponse(NotFound, path) },
  override val pathParamRegexer: PathParamRegexer = registry.require(),
  private val httpExchangeCreator: KFunction<HttpExchange> = HttpExchange::class.primaryConstructor!!,
): RouterConfig(decorators, registry.requireAll(), registry.requireAll()), MutableRegistry by registry {
  init { currentThread().name += "/" + requestIdGenerator.prefix }
  private val http = HttpServer.create()
  private val requestScope = CoroutineScope(SupervisorJob() + workerPool.asCoroutineDispatcher())
  private val logger = logger()

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
    onStopHandlers.reversed().forEach { it.run() }
  }

  // add both Extension and Runnable overloads when this is resolved: https://youtrack.jetbrains.com/issue/KT-56930
  inline fun <reified E: Any> use() = require<E>().also { use(it) }
  fun use(extension: Any) = extension.also {
    register(it)
    when (it) {
      is Extension -> it.install(this)
      is Runnable -> it.run()
      else -> error("Cannot use $it, must be either Extension or Runnable")
    }
  }

  /** Adds a new router context. When handing a request, the longest matching router context is chosen. */
  fun context(prefix: String, block: Router.() -> Unit = {}) =
    Router(prefix, registry, pathParamRegexer, decorators, renderers, parsers).also { router ->
      addContext(prefix, router) { runHandler(this, router.route(this)) }
      router.block()
    }

  fun assets(prefix: String, handler: AssetsHandler) {
    val route = Route(GET, prefix.toRegex(), handler::class.annotations, decorators.wrap(handler))
    addContext(prefix, this, Dispatchers.IO) { runHandler(this, route.takeIf { method == GET || method == HEAD }) }
  }

  private fun addContext(prefix: String, config: RouterConfig, extraCoroutineContext: CoroutineContext = EmptyCoroutineContext, handler: Handler) {
    http.createContext(prefix) { ex ->
      val requestId = requestIdGenerator(ex.requestHeaders)
      requestScope.launch(ThreadNameContext(requestId) + extraCoroutineContext) {
        httpExchangeCreator.call(ex, config, sessionStore, requestId).handler()
      }
    }
  }

  private suspend fun runHandler(exchange: HttpExchange, route: Route?) {
    try {
      if (route != null) exchange.route = route
      val result = (route?.handler ?: notFoundHandler).invoke(exchange)
      if (!exchange.isResponseStarted) exchange.handle(result)
      else if (result != null && result != Unit) logger.warn("Response already started, cannot render $result")
    } catch (ignore: BodyNotAllowedException) {
    } catch (e: Throwable) {
      handleError(exchange, e)
    } finally {
      exchange.close()
    }
  }

  private fun HttpExchange.handle(result: Any?) = when (result) {
    Unit -> send(NoContent)
    is StatusCode -> send(result)
    is ErrorResponse -> render(result.statusCode, result)
    else -> render(OK, result)
  }

  private fun handleError(exchange: HttpExchange, e: Throwable) = try {
    errors.handle(exchange, e)
  } catch (ignore: BodyNotAllowedException) {
  } catch (e2: Throwable) {
    logger.error("While handling $e", e2)
  }
}

fun interface Extension {
  fun install(server: Server)
}

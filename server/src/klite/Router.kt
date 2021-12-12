package klite

import klite.RequestMethod.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class Router(
  val prefix: String,
  val registry: Registry,
  val pathParamRegexer: PathParamRegexer,
  decorators: List<Decorator>,
  bodyRenderers: List<BodyRenderer>,
  bodyParsers: List<BodyParser>
): Registry by registry {
  private val logger = logger()
  private val routes = mutableListOf<Route>()
  private val decorators = decorators.toMutableList()
  internal val bodyRenderers = bodyRenderers.toMutableList()
  internal val bodyParsers = bodyParsers.toMutableList()

  internal fun route(exchange: HttpExchange): Route? {
    val suffix = exchange.path.removePrefix(prefix)
    return match(exchange.method, suffix)?.let { m ->
      exchange.pathParams = m.second.groups
      m.first
    }
  }

  private fun match(method: RequestMethod, path: String): Pair<Route, MatchResult>? {
    for (route in routes) {
      if (method != route.method) continue
      route.path.matchEntire(path)?.let { return route to it }
    }
    return null
  }

  fun add(route: Route) = route.copy(handler = decorators.wrap(route.handler), annotations = route.annotations + handlerAnnotations(route.handler)).also {
    routes += it.apply { logger.info("$method $prefix$path") }
  }

  // TODO: doesn't work for suspend lambda annotations: https://youtrack.jetbrains.com/issue/KT-50200
  private fun handlerAnnotations(handler: Handler) = handler.javaClass.methods.first { !it.isSynthetic }.annotations.toList()

  fun get(path: Regex, handler: Handler) = add(Route(GET, path, handler))
  fun get(path: String = "", handler: Handler) = get(pathParamRegexer.from(path), handler)

  fun post(path: Regex, handler: Handler) = add(Route(POST, path, handler))
  fun post(path: String = "", handler: Handler) = post(pathParamRegexer.from(path), handler)

  fun put(path: Regex, handler: Handler) = add(Route(PUT, path, handler))
  fun put(path: String = "", handler: Handler) = put(pathParamRegexer.from(path), handler)

  fun delete(path: Regex, handler: Handler) = add(Route(DELETE, path, handler))
  fun delete(path: String = "", handler: Handler) = delete(pathParamRegexer.from(path), handler)

  fun decorator(decorator: Decorator) { decorators += decorator }
  fun before(before: Before) = decorator(before.toDecorator())
  fun after(after: After) = decorator(after.toDecorator())

  fun renderer(renderer: BodyRenderer) = bodyRenderers.add(0, renderer)
  fun parser(parser: BodyParser) = bodyParsers.add(0, parser)
}

enum class RequestMethod {
  GET, POST, PUT, DELETE, OPTIONS, HEAD
}

data class Route(val method: RequestMethod, val path: Regex, val handler: Handler, val annotations: List<Annotation> = emptyList())

fun <T: Annotation> Route.annotation(key: KClass<T>) = annotations.find { key.isSuperclassOf(it::class) }
inline fun <reified T: Annotation> Route.annotation() = annotation(T::class)

/** Converts parameterized paths like "/hello/:world/" to Regex with named parameters */
open class PathParamRegexer(private val paramConverter: Regex = "/:([^/]+)".toRegex()) {
  open fun from(path: String) = paramConverter.replace(path, "/(?<$1>[^/]+)").toRegex()
}

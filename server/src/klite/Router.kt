package klite

import klite.RequestMethod.*
import kotlin.reflect.KClass

abstract class RouterConfig(
  decorators: List<Decorator>,
  bodyRenderers: List<BodyRenderer>,
  bodyParsers: List<BodyParser>
) {
  abstract val registry: Registry
  abstract val pathParamRegexer: PathParamRegexer
  val decorators = decorators.toMutableList()
  val renderers = bodyRenderers.toMutableList()
  val parsers = bodyParsers.toMutableList()

  fun decorator(decorator: Decorator) { decorators += decorator }
  fun before(before: Before) = decorator(before.toDecorator())
  fun after(after: After) = decorator(after.toDecorator())

  inline fun <reified T> useOnly() where T: BodyParser, T: BodyRenderer {
    renderers.removeIf { it !is T }
    parsers.removeIf { it !is T }
  }
}

class Router(
  val prefix: String,
  override val registry: Registry,
  override val pathParamRegexer: PathParamRegexer,
  decorators: List<Decorator>,
  renderers: List<BodyRenderer>,
  parsers: List<BodyParser>
): RouterConfig(decorators, renderers, parsers), Registry by registry {
  private val logger = logger()
  private val routes = mutableListOf<Route>()

  internal fun route(exchange: HttpExchange): Route? {
    val suffix = exchange.path.removePrefix(prefix)
    return match(exchange.method, suffix)?.let { m ->
      exchange.pathParams = PathParams(m.second.groups)
      m.first
    }
  }

  private fun match(method: RequestMethod, path: String): Pair<Route, MatchResult>? {
    for (route in routes) {
      if (method == route.method || method == HEAD && route.method == GET)
        route.path.matchEntire(path)?.let { return route to it }
    }
    return null
  }

  fun add(route: Route) = route.copy(handler = decorators.wrap(route.handler), annotations = anonymousHandlerAnnotations(route.handler) + route.annotations).also {
    routes += it.apply { logger.info("$method $prefix$path") }
  }

  // TODO: doesn't work for suspend lambda annotations: https://youtrack.jetbrains.com/issue/KT-50200
  private fun anonymousHandlerAnnotations(handler: Handler) = handler.javaClass.methods.first { !it.isSynthetic }.annotations.toList()

  fun get(path: Regex, handler: Handler) = add(Route(GET, path, handler = handler))
  fun get(path: String = "", handler: Handler) = get(pathParamRegexer.from(path), handler)

  fun post(path: Regex, handler: Handler) = add(Route(POST, path, handler = handler))
  fun post(path: String = "", handler: Handler) = post(pathParamRegexer.from(path), handler)

  fun put(path: Regex, handler: Handler) = add(Route(PUT, path, handler = handler))
  fun put(path: String = "", handler: Handler) = put(pathParamRegexer.from(path), handler)

  fun delete(path: Regex, handler: Handler) = add(Route(DELETE, path, handler = handler))
  fun delete(path: String = "", handler: Handler) = delete(pathParamRegexer.from(path), handler)

  fun options(path: Regex, handler: Handler) = add(Route(OPTIONS, path, handler = handler))
  fun options(path: String = "", handler: Handler) = options(pathParamRegexer.from(path), handler)
}

enum class RequestMethod {
  GET, POST, PUT, DELETE, OPTIONS, HEAD
}

data class Route(val method: RequestMethod, val path: Regex, val annotations: List<Annotation> = emptyList(), val handler: Handler) {
  @Suppress("UNCHECKED_CAST")
  fun <T: Annotation> annotation(key: KClass<T>): T? = annotations.find { key.javaObjectType.isAssignableFrom(it.javaClass) } as? T
  inline fun <reified T: Annotation> annotation() = annotation(T::class)
  inline fun <reified T: Annotation> hasAnnotation() = annotation(T::class) != null
}

/** Converts parameterized paths like "/hello/:world/" to Regex with named parameters */
open class PathParamRegexer(private val paramConverter: Regex = "/:([^/]+)".toRegex()) {
  open fun from(path: String) = paramConverter.replace(path, "/(?<$1>[^/]+)").toRegex()
}

class PathParams(val groups: MatchGroupCollection): Params {
  override val entries get() = throw NotImplementedError()
  override val keys get() = throw NotImplementedError()
  override val values get() = groups.map { it?.value }
  override val size get() = groups.size
  override fun isEmpty() = groups.isEmpty()
  override fun get(key: String) = runCatching { groups[key] }.getOrNull()?.value
  override fun containsKey(key: String) = get(key) != null
  override fun containsValue(value: String?) = groups.find { it != null && it.value == value } != null
}

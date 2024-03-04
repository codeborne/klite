package klite

import klite.RequestMethod.*
import klite.handlers.*
import kotlin.reflect.KAnnotatedElement

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
  inline fun <reified T: Decorator> decorator() = decorator(registry.require<T>())

  fun before(before: Before) = decorator(before.toDecorator())
  inline fun <reified T: Before> before() = before(registry.require<T>())

  fun after(after: After) = decorator(after.toDecorator())
  inline fun <reified T: After> after() = after(registry.require<T>())

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
  private val log = logger()
  val routes = mutableListOf<Route>() // TODO: after Kotlin 2, use immutable getter and mutable setter

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

  fun add(route: Route) = route.apply {
    decoratedHandler = decorators.wrap(route.decoratedHandler)
    routes += this
    log.info("$method $prefix$path")
  }

  fun get(path: Regex, handler: Handler) = add(Route(GET, path, handler = handler))
  fun get(path: String = "", handler: Handler) = get(pathParamRegexer.from(path), handler)

  fun post(path: Regex, handler: Handler) = add(Route(POST, path, handler = handler))
  fun post(path: String = "", handler: Handler) = post(pathParamRegexer.from(path), handler)

  fun put(path: Regex, handler: Handler) = add(Route(PUT, path, handler = handler))
  fun put(path: String = "", handler: Handler) = put(pathParamRegexer.from(path), handler)

  fun patch(path: Regex, handler: Handler) = add(Route(PATCH, path, handler = handler))
  fun patch(path: String = "", handler: Handler) = patch(pathParamRegexer.from(path), handler)

  fun delete(path: Regex, handler: Handler) = add(Route(DELETE, path, handler = handler))
  fun delete(path: String = "", handler: Handler) = delete(pathParamRegexer.from(path), handler)

  fun options(path: Regex, handler: Handler) = add(Route(OPTIONS, path, handler = handler))
  fun options(path: String = "", handler: Handler) = options(pathParamRegexer.from(path), handler)
}

enum class RequestMethod(val hasBody: Boolean = true) {
  GET(false), POST, PUT, PATCH, DELETE(false), OPTIONS, HEAD(false)
}

open class Route(val method: RequestMethod, val path: Regex, annotations: List<Annotation> = emptyList(), val handler: Handler): KAnnotatedElement {
  internal var decoratedHandler: Handler = handler
  override val annotations = anonymousHandlerAnnotations(handler) + annotations

  private fun anonymousHandlerAnnotations(handler: Handler) =
    handler.javaClass.methods.find { it.name.startsWith("invoke") && it.annotations.isNotEmpty() }?.annotations?.toList() ?: emptyList()
}

class NotFoundRoute(path: String, notFoundHandler: Handler): Route(OPTIONS, path.toRegex(), handler = notFoundHandler)

/** Converts parameterized paths like "/hello/:world/" to Regex with named parameters */
open class PathParamRegexer(private val paramConverter: Regex = "(^|/):([^/]+)".toRegex()) {
  open fun from(path: String) = paramConverter.replace(path, "$1(?<$2>[^/]+)").toRegex()
  open fun toOpenApi(path: String) = paramConverter.replace(path, "$1{$2}")
  open fun toOpenApi(path: Regex) = path.pattern.replace("\\(\\?<(.+?)>.*?\\)".toRegex(), "{$1}")
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

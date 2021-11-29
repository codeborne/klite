package server

import server.RequestMethod.*
import server.RequestMethod.GET

class Router(val prefix: String, private val regexer: PathParamRegexer) {
  private val routes = mutableListOf<Route>()

  internal fun route(exchange: HttpExchange): Handler? {
    val suffix = exchange.requestPath.removePrefix(prefix)
    return routes.find { exchange.requestMethod == it.method.name && it.path.matches(suffix) }?.handler
  }

  fun add(route: Route) { routes += route }

  fun get(path: Regex, handler: Handler) = add(Route(GET, path, handler))
  fun get(path: String = "", handler: Handler) = get(regexer.from(path), handler)

  fun post(path: Regex, handler: Handler) = add(Route(POST, path, handler))
  fun post(path: String = "", handler: Handler) = post(regexer.from(path), handler)

  fun put(path: Regex, handler: Handler) = add(Route(PUT, path, handler))
  fun put(path: String = "", handler: Handler) = put(regexer.from(path), handler)

  fun delete(path: Regex, handler: Handler) = add(Route(DELETE, path, handler))
  fun delete(path: String = "", handler: Handler) = delete(regexer.from(path), handler)
}

enum class RequestMethod {
  GET, POST, PUT, DELETE, OPTIONS, HEAD
}

data class Route(val method: RequestMethod, val path: Regex, val handler: Handler)

/** Converts parameterized paths like "/hello/:world/" to Regex with named parameters */
open class PathParamRegexer(private val paramConverter: Regex = "/:([^/]+)".toRegex()) {
  open fun from(path: String) = paramConverter.replace(path, "/(?<$1>[^/]+)").toRegex()
}

typealias Handler = suspend (exchange: HttpExchange) -> Any?

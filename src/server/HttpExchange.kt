package server

typealias HttpExchange = com.sun.net.httpserver.HttpExchange
typealias Headers = com.sun.net.httpserver.Headers

operator fun HttpExchange.get(attr: String): Any? = getAttribute(attr)
operator fun HttpExchange.set(attr: String, value: Any?) = setAttribute(attr, value)

var HttpExchange.pathParams
  get() = get("pathParams") as MatchGroupCollection
  set(value) = set("pathParams", value)

fun HttpExchange.path(param: String) = pathParams[param]?.value ?: error("Param $param missing in path")

operator fun Headers.set(attr: String, value: String) = put(attr, listOf(value))

val HttpExchange.requestPath: String get() = requestURI.path

fun HttpExchange.send(resCode: Int, content: Any? = null, contentType: String? = null) {
  val bytes = when (content) {
    null, Unit -> null
    is ByteArray -> content
    else -> content.toString().toByteArray()
  }
  contentType?.let { responseHeaders["Content-Type"] = if (contentType.startsWith("text")) "$it; charset=UTF-8" else it }
  if (responseCode < 0) sendResponseHeaders(resCode, bytes?.size?.toLong() ?: -1)
  bytes?.let { responseBody.write(it) }
}

package server

import java.net.URI

typealias HttpExchange = com.sun.net.httpserver.HttpExchange
typealias Headers = com.sun.net.httpserver.Headers

@Suppress("UNCHECKED_CAST")
operator fun <T: Any?> HttpExchange.get(attr: String): T = getAttribute(attr) as T
operator fun HttpExchange.set(attr: String, value: Any?) = setAttribute(attr, value)

var HttpExchange.pathParams: MatchGroupCollection
  get() = get("pathParams")
  set(value) = set("pathParams", value)

fun HttpExchange.path(param: String) = pathParams[param]?.value ?: error("Param $param missing in path")

var HttpExchange.queryParams: Map<String, String?>
  get() = get("queryParams") ?: requestURI.queryParams.also { queryParams = it }
  set(value) = set("queryParams", value)

fun HttpExchange.query(param: String) = queryParams[param]

val URI.queryParams: Map<String, String?> get() = rawQuery?.split('&')?.associate {
  it.split('=', limit = 2).let { it[0] to it.getOrNull(1)?.urlDecode() }
} ?: emptyMap()

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

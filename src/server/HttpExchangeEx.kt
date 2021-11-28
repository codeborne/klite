package server

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange

operator fun HttpExchange.get(attr: String): Any? = getAttribute(attr)
operator fun HttpExchange.set(attr: String, value: Any?) = setAttribute(attr, value)

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

package server

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange

operator fun HttpExchange.get(attr: String): Any? = getAttribute(attr)
operator fun HttpExchange.set(attr: String, value: Any?) = setAttribute(attr, value)

operator fun Headers.set(attr: String, value: String) = put(attr, listOf(value))

val HttpExchange.requestPath: String get() = requestURI.path

fun HttpExchange.send(resCode: Int, content: Any?) {
  val bytes = when (content) {
    null -> null
    is ByteArray -> content
    else -> content.toString().toByteArray()
  }
  sendResponseHeaders(resCode, bytes?.size?.toLong() ?: 0)
  if (bytes != null) responseBody.write(bytes)
}

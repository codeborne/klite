package server

import com.sun.net.httpserver.HttpExchange

operator fun HttpExchange.get(attr: String): Any? = getAttribute(attr)
operator fun HttpExchange.set(attr: String, value: Any?) = setAttribute(attr, value)

val HttpExchange.requestPath: String get() = requestURI.path

fun HttpExchange.send(resCode: Int, content: Any?) {
  val bytes = (content?.toString() ?: "").toByteArray()
  sendResponseHeaders(resCode, bytes.size.toLong())
  responseBody.write(bytes)
}

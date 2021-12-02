package klite

import java.io.InputStream
import java.io.OutputStream
import java.net.URI

typealias OriginalHttpExchange = com.sun.net.httpserver.HttpExchange
typealias Headers = com.sun.net.httpserver.Headers

class HttpExchange(private val original: OriginalHttpExchange): AutoCloseable {
  val method = RequestMethod.valueOf(original.requestMethod)
  val remoteAddress: String get() = original.remoteAddress.address.hostAddress // TODO: x-forwarded-for support

  // TODO: defaultContentType or look into Accept header
  // TODO: getRequestURL (full)
  // TODO: cookies
  // TODO: session

  val path: String get() = original.requestURI.path
  lateinit var pathParams: MatchGroupCollection internal set
  fun path(param: String) = pathParams[param]?.value ?: error("Param $param missing in path")

  val query: String get() = original.requestURI.query?.let { "?$it" } ?: ""
  val queryParams: Map<String, String?> by lazy { original.requestURI.queryParams }
  fun query(param: String) = queryParams[param]

  val attrs: MutableMap<Any, Any?> = mutableMapOf()
  @Suppress("UNCHECKED_CAST")
  fun <T> attr(key: Any): T = attrs[key] as T
  fun attr(key: Any, value: Any?) = attrs.put(key, value)

  val headers: Headers get() = original.requestHeaders
  fun header(key: String): String? = headers[key]?.firstOrNull()

  val responseHeaders: Headers get() = original.responseHeaders
  fun header(key: String, value: String?) { responseHeaders[key] = value }

  val statusCode: Int get() = original.responseCode
  val isResponseStarted get() = statusCode >= 0

  val requestStream: InputStream get() = original.requestBody
  val responseStream: OutputStream get() = original.responseBody

  var responseType: String?
    get() = responseHeaders["Content-Type"]?.firstOrNull()
    set(value) = header("Content-Type", value)

  fun accept(contentType: String) = header("Accept")?.contains(contentType) ?: true

  fun send(resCode: Int, content: Any? = null, contentType: String? = null) {
    val bytes = when (content) {
      null, Unit -> null
      is ByteArray -> content
      else -> content.toString().toByteArray()
    }
    contentType?.let { responseType = if (contentType.startsWith("text")) "$it; charset=UTF-8" else it }
    if (statusCode < 0) original.sendResponseHeaders(resCode, bytes?.size?.toLong() ?: -1)
    bytes?.let { responseStream.write(it) }
  }

  private val onCompleteHandlers = mutableListOf<Runnable>()
  fun onComplete(handler: Runnable) { onCompleteHandlers += handler }

  override fun close() {
    original.close()
    onCompleteHandlers.forEach { it.run() }
  }

  override fun toString() = "$method ${original.requestURI}"
}

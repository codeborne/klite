package klite

import java.io.InputStream
import java.io.OutputStream

typealias OriginalHttpExchange = com.sun.net.httpserver.HttpExchange
typealias Headers = com.sun.net.httpserver.Headers

class HttpExchange(private val original: OriginalHttpExchange, val bodyRenderer: BodyRenderer): AutoCloseable {
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
  fun header(key: String, value: String) { responseHeaders[key] = value }

  val statusCode: Int get() = original.responseCode
  val isResponseStarted get() = statusCode >= 0

  val requestStream: InputStream get() = original.requestBody
  val responseStream: OutputStream get() = original.responseBody

  var responseType: String?
    get() = responseHeaders["Content-Type"]?.firstOrNull()
    set(value) { value?.let { header("Content-Type", it) } }

  fun accept(contentType: String) = header("Accept")?.contains(contentType) ?: true

  fun render(resCode: Int, body: Any?) {
    responseType = bodyRenderer.contentType
    original.sendResponseHeaders(resCode, 0)
    bodyRenderer.render(responseStream, body)
  }

  fun send(resCode: Int, body: ByteArray? = null, contentType: String? = null) {
    responseType = contentType
    original.sendResponseHeaders(resCode, body?.size?.toLong() ?: -1)
    body?.let { responseStream.write(it) }
  }

  fun send(resCode: Int, body: String?, contentType: String? = null) =
    send(resCode, body?.toByteArray(), "$contentType; charset=UTF-8")

  private val onCompleteHandlers = mutableListOf<Runnable>()
  fun onComplete(handler: Runnable) { onCompleteHandlers += handler }

  override fun close() {
    original.close()
    onCompleteHandlers.forEach { it.run() }
  }

  override fun toString() = "$method ${original.requestURI}"
}

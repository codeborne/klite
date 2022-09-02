package klite

import com.sun.net.httpserver.HttpsExchange
import klite.RequestMethod.HEAD
import klite.StatusCode.Companion.Found
import klite.StatusCode.Companion.OK
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KClass

typealias OriginalHttpExchange = com.sun.net.httpserver.HttpExchange
typealias Headers = com.sun.net.httpserver.Headers

open class HttpExchange(
  private val original: OriginalHttpExchange,
  private val config: RouterConfig,
  private val sessionStore: SessionStore?,
  val requestId: String
): AutoCloseable {
  lateinit var route: Route
  val method = RequestMethod.valueOf(original.requestMethod)
  open val remoteAddress: String get() = original.remoteAddress.address.hostAddress
  open val host: String get() = header("Host")!!
  open val isSecure: Boolean get() = original is HttpsExchange

  val path: String get() = original.requestURI.path
  lateinit var pathParams: Params internal set
  fun path(param: String) = pathParams[param] ?: error("Param $param missing in path")

  val query: String get() = original.requestURI.query?.let { "?$it" } ?: ""
  val queryParams: Params by lazy { original.requestURI.queryParams }
  fun query(param: String) = queryParams[param]

  val fullUrl get() = fullUrl(original.requestURI.toString())
  fun fullUrl(suffix: String): URI = URI("http${if (isSecure) "s" else ""}://$host$suffix")

  inline fun <reified T: Any> body(): T = body(T::class)
  fun <T: Any> body(type: KClass<T>): T {
    val contentType = requestType ?: "text/plain"
    return config.parsers.find { contentType.startsWith(it.contentType) }?.parse(requestStream, type) ?:
      throw UnsupportedMediaTypeException(requestType)
  }
  /** Note: this can be called only once */
  val rawBody: String get() = requestStream.use { it.readBytes().decodeToString() }

  val bodyParams: Params by lazy { body() }
  fun body(param: String) = bodyParams[param]

  val attrs: MutableMap<Any, Any?> = mutableMapOf()
  @Suppress("UNCHECKED_CAST")
  fun <T> attr(key: Any): T = attrs[key] as T
  fun attr(key: Any, value: Any?) = attrs.put(key, value)

  val headers: Headers get() = original.requestHeaders
  fun header(key: String): String? = headers.getFirst(key)

  val responseHeaders: Headers get() = original.responseHeaders
  fun header(key: String, value: String) { responseHeaders[key] = value }

  val cookies: Params by lazy(NONE) { decodeCookies(header("Cookie")) }
  fun cookie(key: String) = cookies[key]

  fun cookie(key: String, value: String, expires: Instant? = null) { this += Cookie(key, value, expires, secure = isSecure) }
  operator fun plusAssign(cookie: Cookie) = responseHeaders.add("Set-Cookie", cookie.toString())

  val session: Session by lazy(NONE) { sessionStore?.load(this) ?: error("No sessionStore defined") }

  val statusCode: Int get() = original.responseCode
  val isResponseStarted get() = statusCode >= 0

  val requestType get() = header("Content-Type")
  val requestStream: InputStream get() = original.requestBody

  var responseType: String?
    get() = responseHeaders["Content-Type"]?.firstOrNull()
    set(value) { value?.let { header("Content-Type", it) } }

  val accept get() = Accept(header("Accept"))

  fun render(code: StatusCode, body: Any?) {
    val accept = accept
    val renderer = config.renderers.find { accept(it) } ?:
      if (accept.isRelaxed || code != OK) config.renderers.first() else throw NotAcceptableException(accept.contentTypes)
    val out = startResponse(code, contentType = renderer.contentType)
    if (method != HEAD) renderer.render(out, body)
  }

  /**
   * Sends response headers and provides the stream
   * @param length 0 -> no response, null -> use chunked encoding
   */
  fun startResponse(code: StatusCode, length: Long? = null, contentType: String? = null): OutputStream {
    sessionStore?.save(this, session)
    responseType = contentType
    original.sendResponseHeaders(code.value, if (length == 0L) -1 else length ?: 0)
    return original.responseBody
  }

  fun send(code: StatusCode, body: ByteArray? = null, contentType: String? = null) {
    val out = startResponse(code, body?.size?.toLong().takeIf { method != HEAD } ?: 0, contentType)
    if (method != HEAD) body?.let { out.write(it) }
  }

  fun send(code: StatusCode, body: String?, contentType: String? = null) =
    send(code, body?.toByteArray(), "$contentType; charset=UTF-8")

  fun redirect(url: URI, statusCode: StatusCode = Found): Nothing = redirect(url.toString(), statusCode)
  fun redirect(location: String, statusCode: StatusCode = Found): Nothing {
    header("Location", location)
    throw StatusCodeException(statusCode)
  }

  private val onCompleteHandlers = mutableListOf<Runnable>()
  fun onComplete(handler: Runnable) { onCompleteHandlers += handler }

  override fun close() {
    original.close()
    onCompleteHandlers.forEach { it.run() }
  }

  override fun toString() = "$method ${original.requestURI}"
}

operator fun Headers.plusAssign(headers: Map<String, String>) = headers.forEach { (k, v) -> set(k, v) }

open class RequestIdGenerator {
  val prefix = (0xFFFF * Math.random()).toInt().toString(16)
  private val counter = AtomicLong()
  open operator fun invoke(headers: Headers) = "$prefix-${counter.incrementAndGet()}"
}

open class XRequestIdGenerator: RequestIdGenerator() {
  override fun invoke(headers: Headers) = super.invoke(headers) + (headers.getFirst("X-Request-Id")?.let { "/$it" } ?: "")
}

// TODO: maybe a separate interface inside of HttpExchange?
class XForwardedHttpExchange(original: OriginalHttpExchange, config: RouterConfig, sessionStore: SessionStore?, requestId: String):
  HttpExchange(original, config, sessionStore, requestId) {
  companion object {
    private val forwardedIPIndexFromEnd = Config.optional("XFORWARDED_IP_FROM_END", "1").toInt()
  }
  override val remoteAddress get() = header("X-Forwarded-For")?.split(", ")?.let { it.getOrNull(it.size - forwardedIPIndexFromEnd) } ?: super.remoteAddress
  override val host get() = header("X-Forwarded-Host") ?: super.host
  override val isSecure get() = header("X-Forwarded-Proto") == "https"
}

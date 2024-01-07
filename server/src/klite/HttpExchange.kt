package klite

import com.sun.net.httpserver.HttpsExchange
import klite.RequestMethod.HEAD
import klite.RequestMethod.OPTIONS
import klite.StatusCode.Companion.Found
import klite.StatusCode.Companion.OK
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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
  open val protocol: String get() = if (isSecure) "https" else "http"

  val path: String get() = original.requestURI.path
  lateinit var pathParams: Params internal set
  fun path(param: String): String? = pathParams[param]

  val query: String get() = original.requestURI.query?.let { "?$it" } ?: ""
  val queryParams: Params by lazy { original.requestURI.queryParams }
  fun query(param: String): String? = queryParams[param]

  val fullUrl get() = fullUrl(original.requestURI.toString())
  fun fullUrl(suffix: String): URI = URI("$protocol://$host$suffix")

  inline fun <reified T: Any> body(): T = body(typeOf<T>())

  @Suppress("UNCHECKED_CAST")
  fun <T: Any> body(type: KType): T {
    if (type.classifier == String::class) return rawBody as T
    if (type.classifier == ByteArray::class) return requestStream.readBytes() as T
    val contentType = requestType ?: MimeTypes.text
    val bodyParser = config.parsers.find { contentType.startsWith(it.contentType) } ?: throw UnsupportedMediaTypeException(requestType)
    return bodyParser.parse(this, type)
  }

  /** Note: this can be called only once */
  val rawBody: String get() = requestStream.reader().use { it.readText() }

  val bodyParams: Map<String, Any?> by lazy { body() }
  /** e.g. form param, passed in body */ @Suppress("UNCHECKED_CAST")
  fun <T> body(param: String): T = bodyParams[param] as T

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

  fun cookie(key: String, value: String, expires: Instant? = null) = cookie(Cookie(key, value, expires, secure = isSecure))
  fun cookie(cookie: Cookie) = responseHeaders.add("Set-Cookie", cookie.toString())
  operator fun plusAssign(cookie: Cookie) = cookie(cookie)

  val session: Session by lazy(NONE) { sessionStore?.load(this) ?: error("No sessionStore defined") }

  val statusCode: StatusCode? get() = StatusCode(original.responseCode).takeIf { isResponseStarted }
  val isResponseStarted get() = original.responseCode >= 0
  var failure: Throwable? = null

  val requestType: String? get() = header("Content-Type")
  val requestStream: InputStream get() = if (method.hasBody) original.requestBody else error("$method should not have body")

  var responseType: String?
    get() = responseHeaders["Content-type"]?.firstOrNull()
    set(value) { value?.let { header("Content-type", MimeTypes.withCharset(it)) } }

  fun fileName(fileName: String, attachment: Boolean = true) {
    header("Content-disposition", (if (attachment) "attachment; " else "") + "filename=\"$fileName\"")
    responseType = MimeTypes.typeFor(fileName)
  }

  val accept get() = Accept(header("Accept"))

  fun render(code: StatusCode, body: Any?) = findRenderer(takeFirst = code != OK).render(this, code, body)

  private fun findRenderer(takeFirst: Boolean = false): BodyRenderer {
    val accept = accept
    return config.renderers.find { accept(it) }
      ?: if (accept.isRelaxed || takeFirst) config.renderers.first() else throw NotAcceptableException(accept.contentTypes)
  }

  /**
   * Sends response headers and provides the stream
   * @param length 0 -> no response, null -> use chunked encoding
   */
  fun startResponse(code: StatusCode, length: Long? = null, contentType: String? = null, fileName: String? = null): OutputStream {
    sessionStore?.save(this, session)
    fileName?.let { fileName(it) }
    contentType?.let { responseType = it }
    val bodyNotAllowed = method == HEAD || method == OPTIONS
    original.sendResponseHeaders(code.value, if (length == 0L || bodyNotAllowed) -1 else length ?: 0)
    if (bodyNotAllowed) throw BodyNotAllowedException()
    return original.responseBody
  }

  fun send(code: StatusCode, body: ByteArray? = null, contentType: String? = null) {
    val out = startResponse(code, body?.size?.toLong() ?: 0, contentType)
    body?.let { out.write(it) }
  }

  fun send(code: StatusCode, body: String?, contentType: String? = null) =
    send(code, body?.toByteArray(), contentType)

  /** Use in a GET handler to implement SSE (Server-Sent Events), follow by [send] calls */
  fun startEventStream() = startResponse(OK, null, MimeTypes.eventStream)
  fun send(event: ServerSentEvent, dataRenderer: BodyRenderer? = config.renderers.first()) {
    if (!isResponseStarted) startEventStream()
    event.sendTo(original.responseBody, dataRenderer)
  }

  fun redirect(location: String, statusCode: StatusCode = Found): Nothing = throw RedirectException(location, statusCode)
  fun redirect(url: URI, statusCode: StatusCode = Found): Nothing = redirect(url.toString(), statusCode)

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

class XForwardedHttpExchange(original: OriginalHttpExchange, config: RouterConfig, sessionStore: SessionStore?, requestId: String):
  HttpExchange(original, config, sessionStore, requestId) {
  companion object {
    private val forwardedIPIndexFromEnd = Config.optional("XFORWARDED_IP_FROM_END", "1").toInt()
  }
  override val remoteAddress get() = header("X-Forwarded-For")?.split(", ")?.let { it.getOrNull(it.size - forwardedIPIndexFromEnd) } ?: super.remoteAddress
  override val host get() = header("X-Forwarded-Host") ?: super.host
  override val protocol get() = header("X-Forwarded-Proto") ?: "http"
  override val isSecure get() = protocol == "https"
}

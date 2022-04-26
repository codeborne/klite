package klite.json

import com.fasterxml.jackson.databind.json.JsonMapper
import klite.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.ofString
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.time.Duration
import java.time.Duration.ofSeconds
import kotlin.reflect.KClass

typealias RequestModifier =  HttpRequest.Builder.() -> HttpRequest.Builder

/**
 * Configure a default java.net.HttpClient in your registry, e.g.
 * registry.register(HttpClient.newBuilder().connectTimeout(ofSeconds(5)).build())
 */
class JsonHttpClient(
  registry: Registry,
  private val urlPrefix: String = "",
  val reqModifier: RequestModifier = { this },
  val errorHandler: (HttpResponse<*>, String) -> Nothing = { res, body -> throw IOException("Failed with ${res.statusCode()}: $body") },
  val retryCount: Int = 0,
  val retryAfter: Duration = ofSeconds(1),
  private val maxLoggedLen: Int = 1000,
  val http: HttpClient = registry.require(),
  val json: JsonMapper = registry.require()
) {
  val logger = logger(Exception().stackTrace.first { it.className !== javaClass.name }.className).apply {
    info("Using $urlPrefix")
  }

  private fun jsonReq(urlSuffix: String) = HttpRequest.newBuilder().uri(URI("$urlPrefix$urlSuffix"))
    .setHeader("Content-Type", "application/json; charset=UTF-8").setHeader("Accept", "application/json")
    .timeout(ofSeconds(10)).reqModifier()

  private suspend fun <T: Any> request(urlSuffix: String, type: KClass<T>, payload: String? = null, builder: RequestModifier): T {
    val req = jsonReq(urlSuffix).builder().build()
    val start = System.nanoTime()
    val res = http.sendAsync(req, ofString()).await()
    val ms = (System.nanoTime() - start) / 1000_000
    val body = res.body().trim() // TODO: NPE -> return nullable type
    if (res.statusCode() < 300) {
      logger.info("${req.method()} $urlSuffix ${cut(payload)} in $ms ms: ${cut(body)}")
      @Suppress("UNCHECKED_CAST") return when (type) {
        Unit::class -> Unit as T
        String::class -> body as T
        else -> json.parse(body, type)
      }
    }
    else {
      logger.error("Failed ${req.method()} $urlSuffix ${cut(payload)} in $ms ms: ${res.statusCode()}: $body")
      errorHandler(res, body)
    }
  }

  private fun cut(s: String?) = if (s == null) "" else if (s.length <= maxLoggedLen) s else s.substring(0, maxLoggedLen) + "..."

  suspend fun <T: Any> retryRequest(urlSuffix: String, type: KClass<T>, payload: String? = null, builder: RequestModifier): T {
    for (i in 0..retryCount) {
      try {
        return request(urlSuffix, type, payload, builder)
      } catch (e: IOException) {
        if (i < retryCount) {
          logger.error("Failed $urlSuffix, retry ${i + 1} after $retryAfter", e)
          delay(retryAfter.toMillis())
        }
        else {
          logger.error("Failed $urlSuffix: ${cut(payload)}", e)
          throw e
        }
      }
    }
    error("Unreachable")
  }

  suspend inline fun <reified T: Any> request(urlSuffix: String, noinline builder: RequestModifier) = retryRequest(urlSuffix, T::class, builder = builder)

  suspend fun <T: Any> get(urlSuffix: String, type: KClass<T>) = retryRequest(urlSuffix, type) { GET() }
  suspend inline fun <reified T: Any> get(urlSuffix: String) = get(urlSuffix, T::class)

  suspend fun <T: Any> post(urlSuffix: String, o: Any?, type: KClass<T>) = toJson(o).let { retryRequest(urlSuffix, type, it) { POST(ofString(it)) } }
  suspend inline fun <reified T: Any> post(urlSuffix: String, o: Any?) = post(urlSuffix, o, T::class)

  suspend fun <T: Any> put(urlSuffix: String, o: Any?, type: KClass<T>) = toJson(o).let { retryRequest(urlSuffix, type, it) { PUT(ofString(it)) } }
  suspend inline fun <reified T: Any> put(urlSuffix: String, o: Any?) = put(urlSuffix, o, T::class)

  suspend fun <T: Any> delete(urlSuffix: String, type: KClass<T>) = retryRequest(urlSuffix, type) { DELETE() }
  suspend inline fun <reified T: Any> delete(urlSuffix: String) = delete(urlSuffix, T::class)

  private fun toJson(o: Any?) = if (o is String) o else json.stringify(o)
}

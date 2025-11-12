package klite.http

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

inline fun httpClient(builder: HttpClient.Builder.() -> Unit = {}): HttpClient =
  HttpClient.newBuilder().connectTimeout(5.seconds).apply(builder).build()

fun HttpClient.Builder.connectTimeout(duration: Duration): HttpClient.Builder = connectTimeout(duration.toJavaDuration())

fun HttpRequest.Builder.timeout(duration: Duration): HttpRequest.Builder = timeout(duration.toJavaDuration())
fun HttpRequest.Builder.authBearer(token: String) = setHeader("Authorization", "Bearer $token")
fun HttpRequest.Builder.contentType(mimeType: String) = setHeader("Content-Type", mimeType)
fun HttpRequest.Builder.accept(mimeType: String) = setHeader("Accept", mimeType)

typealias RequestModifier = HttpRequest.Builder.() -> HttpRequest.Builder

suspend fun <R> HttpClient.request(url: URI, bodyHandler: HttpResponse.BodyHandler<R>, modifier: RequestModifier = { this }) =
  sendAsync(HttpRequest.newBuilder().uri(url).timeout(10.seconds).modifier().build(), bodyHandler).await()

suspend fun HttpClient.get(url: URI, modifier: RequestModifier) = request(url, ofString()) { GET().modifier() }
suspend fun HttpClient.post(url: URI, data: Any?, modifier: RequestModifier) = request(url, ofString()) { POST(toBodyPublisher(data)).modifier() }
suspend fun HttpClient.put(url: URI, data: Any?, modifier: RequestModifier) = request(url, ofString()) { PUT(toBodyPublisher(data)).modifier() }
suspend fun HttpClient.patch(url: URI, data: Any?, modifier: RequestModifier) = request(url, ofString()) { method("PATCH", toBodyPublisher(data)).modifier() }
suspend fun HttpClient.delete(url: URI, modifier: RequestModifier) = request(url, ofString()) { DELETE().modifier() }

fun toBodyPublisher(data: Any?): HttpRequest.BodyPublisher = when (data) {
  null, Unit -> BodyPublishers.noBody()
  is ByteArray -> BodyPublishers.ofByteArray(data)
  else -> BodyPublishers.ofString(data.toString())
}

package klite.json

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import klite.SimpleRegistry
import klite.register
import kotlinx.coroutines.runBlocking
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.time.Duration.ofSeconds
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.failedFuture

class JsonHttpClientTest {
  val httpClient = mockk<HttpClient>()
  val registry = SimpleRegistry().apply {
    register(httpClient)
    register(buildMapper())
  }
  val http = JsonHttpClient(registry, "http://some.host/v1", reqModifier = { setHeader("X-Custom-API", "123") },
    retryCount = 2, retryAfter = ofSeconds(0))

  @Test fun get() {
    val response = mockResponse(200, """{"hello": "World"}""")
    every { httpClient.sendAsync<String>(any(), any()) } returns completedFuture(response)

    runBlocking {
      val data = http.get<SomeData>("/some/data")
      expect(data).to.equal(SomeData("World"))
    }

    coVerify { httpClient.sendAsync<String>(match { it.uri().toString() == "http://some.host/v1/some/data" }, any()) }
  }

  @Test fun `http error`() {
    val response = mockResponse(500, """{"error": "Error"}""")
    every { httpClient.sendAsync<String>(any(), any()) } returns completedFuture(response)

    assertThrows<IOException> { runBlocking { http.get<SomeData>("/error") } }
  }

  @Test fun exception() {
    val exception = IOException()
    every { httpClient.sendAsync<String>(any(), any()) }.returnsMany(failedFuture(exception))
    assertThrows<IOException> { runBlocking { http.post<String>("/some/data", "Hello") } }
    coVerify(exactly = 3) { httpClient.sendAsync<String>(any(), any()) }
  }

  @Test fun retry() {
    val response = mockResponse(200, """{"hello": "World"}""")
    every { httpClient.sendAsync<String>(any(), any()) }.returnsMany(failedFuture(IOException()), completedFuture(response))
    runBlocking {
      val body = http.post<String>("/some/data", "Hello")
      expect(body).to.equal(response.body())
    }
    coVerify(exactly = 2) { httpClient.sendAsync<String>(any(), any()) }
  }

  private fun mockResponse(status: Int, body: String) = mockk<HttpResponse<String>> {
    every { statusCode() } returns status
    every { body() } returns body
  }
}

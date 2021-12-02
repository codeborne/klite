package server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class HttpExchangeTest {
  abstract class MockableHttpExchange: OriginalHttpExchange()
  val original = mockk<MockableHttpExchange>(relaxed = true) {
    every { requestMethod } returns "GET"
    every { requestURI } returns URI("/hello?hello=world")
    every { responseCode } returns -1
  }
  val exchange = HttpExchange(original)

  @Test
  fun path() {
    assertThat(exchange.method).isEqualTo(RequestMethod.GET)
    assertThat(exchange.path).isEqualTo("/hello")
  }

  @Test
  fun queryParams() {
    assertThat(exchange.query).isEqualTo("?hello=world")
    assertThat(exchange.queryParams).isEqualTo(mapOf("hello" to "world"))
  }

  @Test
  fun `send text`() {
    exchange.send(200, "Hello", "text/custom")
    verify {
      original.responseHeaders["Content-Type"] = "text/custom; charset=UTF-8"
      original.sendResponseHeaders(200, 5)
      original.responseBody.write("Hello".toByteArray())
    }
  }

  @Test
  fun `send binary`() {
    exchange.send(201, "XXX".toByteArray(), "image/custom")
    verify {
      original.responseHeaders["Content-Type"] = "image/custom"
      original.sendResponseHeaders(201, 3)
      original.responseBody.write("XXX".toByteArray())
    }
  }

  @Test
  fun onComplete() {
    val handler1 = mockk<Runnable>(relaxed = true)
    val handler2 = mockk<Runnable>(relaxed = true)
    exchange.onComplete(handler1)
    exchange.onComplete(handler2)

    exchange.close()

    verify {
      original.close()
      handler1.run()
      handler2.run()
    }
  }
}

package server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class HttpExchangeTest {
  abstract class MockableHttpExchange: OriginalHttpExchange()
  val original = mockk<MockableHttpExchange>(relaxed = true) {
    every { requestMethod } returns "GET"
    every { requestURI } returns URI("/hello?hello=world")
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

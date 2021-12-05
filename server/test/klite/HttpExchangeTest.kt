package klite

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.Cookie.SameSite.Strict
import klite.annotations.TypeConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI

class HttpExchangeTest {
  abstract class MockableHttpExchange: OriginalHttpExchange()
  val original = mockk<MockableHttpExchange>(relaxed = true) {
    every { requestMethod } returns "GET"
    every { requestURI } returns URI("/hello?hello=world")
    every { responseCode } returns -1
  }
  val bodyRenderer = mockk<BodyRenderer>()
  val customParser = mockk<BodyParser> {
    every { contentType } returns "application/specific"
  }
  val exchange = HttpExchange(original, listOf(bodyRenderer), listOf(TextBodyParser(TypeConverter()), customParser))

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
  fun fullUrl() {
    every { exchange.host } returns "localhost:8080"
    assertThat(exchange.fullUrl).isEqualTo(URI("http://localhost:8080/hello?hello=world"))

    every { exchange.host } returns "host.domain"
    assertThat(exchange.fullUrl("/some/page")).isEqualTo(URI("http://host.domain/some/page"))
  }

  @Test
  fun `request cookies`() {
    every { exchange.header("Cookie") } returns "Hello=World; Second=123%20456"
    assertThat(exchange.cookies).isEqualTo(mapOf("Hello" to "World", "Second" to "123 456"))
    assertThat(exchange.cookie("Hello")).isEqualTo("World")
  }

  @Test
  fun `response cookies`() {
    exchange.cookie("Hello", "World")
    verify { original.responseHeaders.add("Set-Cookie", "Hello=World") }

    exchange += Cookie("LANG", "et", sameSite = Strict, domain = "angryip.org")
    verify { original.responseHeaders.add("Set-Cookie", "LANG=et; Domain=angryip.org; SameSite=Strict") }
  }

  @Test
  fun `body as text`() {
    every { exchange.requestType } returns null
    every { original.requestBody } answers { "123".byteInputStream() }
    assertThat(exchange.body<String>()).isEqualTo("123")
    assertThat(exchange.body<Int>()).isEqualTo(123)
  }

  @Test
  fun `body with specific content-type`() {
    every { exchange.requestType } returns customParser.contentType
    val input = "{PI}".byteInputStream()
    every { original.requestBody } answers { input }
    every { customParser.parse(input, Double::class) } returns Math.PI
    assertThat(exchange.body<Double>()).isEqualTo(Math.PI)
  }

  @Test
  fun `body with unsupported content-type`() {
    every { exchange.requestType } returns "unsupported"
    every { original.requestBody } returns "".byteInputStream()
    assertThrows<UnsupportedMediaTypeException> { exchange.body<String>() }
  }

  @Test
  fun `send text`() {
    exchange.send(StatusCode.OK, "Hello", "text/custom")
    verify {
      original.responseHeaders["Content-Type"] = "text/custom; charset=UTF-8"
      original.sendResponseHeaders(200, 5)
      original.responseBody.write("Hello".toByteArray())
    }
  }

  @Test
  fun `send binary`() {
    exchange.send(StatusCode.Created, "XXX".toByteArray(), "image/custom")
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

package klite

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.Cookie.SameSite.Strict
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
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
  val routerConfig = mockk<RouterConfig> {
    every { parsers } returns mutableListOf(TextBodyParser(), customParser)
    every { renderers } returns mutableListOf(bodyRenderer)
  }
  val exchange = HttpExchange(original, routerConfig, null)

  @Test fun path() {
    expect(exchange.method).to.equal(RequestMethod.GET)
    expect(exchange.path).to.equal("/hello")
  }

  @Test fun queryParams() {
    expect(exchange.query).to.equal("?hello=world")
    expect(exchange.queryParams).to.equal(mapOf("hello" to "world"))
  }

  @Test fun fullUrl() {
    every { exchange.host } returns "localhost:8080"
    expect(exchange.fullUrl).to.equal(URI("http://localhost:8080/hello?hello=world"))

    every { exchange.host } returns "host.domain"
    expect(exchange.fullUrl("/some/page")).to.equal(URI("http://host.domain/some/page"))
  }

  @Test fun `request cookies`() {
    every { exchange.header("Cookie") } returns "Hello=World; Second=123%20456"
    expect(exchange.cookies).to.equal(mapOf("Hello" to "World", "Second" to "123 456"))
    expect(exchange.cookie("Hello")).to.equal("World")
  }

  @Test fun `response cookies`() {
    exchange.cookie("Hello", "World")
    verify { original.responseHeaders.add("Set-Cookie", "Hello=World") }

    exchange += Cookie("LANG", "et", sameSite = Strict, domain = "angryip.org")
    verify { original.responseHeaders.add("Set-Cookie", "LANG=et; Domain=angryip.org; SameSite=Strict") }
  }

  @Test fun `body as text`() {
    every { exchange.requestType } returns null
    every { original.requestBody } answers { "123".byteInputStream() }
    expect(exchange.body<String>()).to.equal("123")
    expect(exchange.body<Int>()).to.equal(123)
  }

  @Test fun `body with specific content-type`() {
    every { exchange.requestType } returns customParser.contentType
    val input = "{PI}".byteInputStream()
    every { original.requestBody } answers { input }
    every { customParser.parse(input, Double::class) } returns Math.PI
    expect(exchange.body<Double>()).to.equal(Math.PI)
  }

  @Test fun `body with unsupported content-type`() {
    every { exchange.requestType } returns "unsupported"
    every { original.requestBody } returns "".byteInputStream()
    assertThrows<UnsupportedMediaTypeException> { exchange.body<String>() }
  }

  @Test fun `send text`() {
    exchange.send(StatusCode.OK, "Hello", "text/custom")
    verify {
      original.responseHeaders["Content-Type"] = "text/custom; charset=UTF-8"
      original.sendResponseHeaders(200, 5)
      original.responseBody.write("Hello".toByteArray())
    }
  }

  @Test fun `send binary`() {
    exchange.send(StatusCode.Created, "XXX".toByteArray(), "image/custom")
    verify {
      original.responseHeaders["Content-Type"] = "image/custom"
      original.sendResponseHeaders(201, 3)
      original.responseBody.write("XXX".toByteArray())
    }
  }

  @Test fun onComplete() {
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

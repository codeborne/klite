package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.IOException

class DecoratorsTest {
  val exchange = mockk<HttpExchange>()
  val handler = mockk<Handler>().also {
    coEvery { exchange.it() } returns "Result"
  }

  @Test fun before() {
    val before = mockk<Before>(relaxed = true)
    val wrapped = before.toDecorator().wrap(handler)
    expect(runBlocking { wrapped(exchange) }).toEqual("Result")
    coVerify { before.before(exchange) }
  }

  @Test fun after() {
    val after = mockk<After>(relaxed = true)
    val wrapped = after.toDecorator().wrap(handler)
    expect(runBlocking { wrapped(exchange) }).toEqual("Result")
    coVerify { after.after(exchange, null) }
  }

  @Test fun `after with exception`() {
    val after = mockk<After>(relaxed = true)
    coEvery { handler(exchange) } throws IOException()
    val wrapped = after.toDecorator().wrap(handler)
    expect { runBlocking { wrapped(exchange) }}.toThrow<IOException>()
    coVerify { after.after(exchange, any<IOException>()) }
  }

  @Test fun around() {
    val around: Decorator = { ex, h -> "<<${h(ex)}>>" }
    val wrapped = around.wrap(handler)
    expect(runBlocking { wrapped(exchange) }).toEqual("<<Result>>")
  }
}

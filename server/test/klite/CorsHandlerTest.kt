package klite

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.RequestMethod.OPTIONS
import klite.RequestMethod.POST
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CorsHandlerTest {
  val exchange = mockk<HttpExchange>(relaxed = true)
  val cors = CorsHandler()

  @Test fun `no origin`() {
    every { exchange.header("Origin") } returns null
    runBlocking { cors.before(exchange) }
    verify(exactly = 0) { exchange.header(any(), any()) }
  }

  @Test fun `allow any origin`() {
    every { exchange.header("Origin") } returns "my.origin"
    runBlocking { cors.before(exchange) }
    verify { exchange.header("Allow-Origin", "my.origin") }
  }

  @Test fun `allow only specific origin`() {
    val cors = CorsHandler(allowedOrigins = setOf("my.origin"))
    every { exchange.header("Origin") } returns "my.origin"
    runBlocking { cors.before(exchange) }
    verify { exchange.header("Allow-Origin", "my.origin") }

    every { exchange.header("Origin") } returns "other.origin"
    runBlocking { assertThrows<ForbiddenException> { cors.before(exchange) } }
  }

  @Test fun `preflight request`() {
    every { exchange.method } returns OPTIONS
    every { exchange.header("Origin") } returns "my.origin"
    every { exchange.header("Access-Control-Request-Method") } returns POST.toString()
    every { exchange.header("Access-Control-Request-Headers") } returns "Custom-Header"

    runBlocking { assertThrows<BodyNotAllowedException> { cors.before(exchange) } }

    verify {
      exchange.header("Access-Control-Allow-Methods", "GET, POST")
      exchange.header("Access-Control-Allow-Headers", "Custom-Header")
      exchange.header("Access-Control-Max-Age", cors.maxAgeSec.toString())
    }
  }
}

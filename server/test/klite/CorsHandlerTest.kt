package klite

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.RequestMethod.OPTIONS
import klite.RequestMethod.POST
import klite.StatusCode.Companion.OK
import klite.handlers.CorsHandler
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CorsHandlerTest {
  val exchange = mockk<HttpExchange>(relaxed = true)
  val cors = CorsHandler()

  @Test fun `no origin`() = runTest {
    every { exchange.header("Origin") } returns null
    cors.run { exchange.before() }
    verify(exactly = 0) { exchange.header(any(), any()) }
  }

  @Test fun `allow any origin`() = runTest {
    every { exchange.header("Origin") } returns "my.origin"
    cors.run { exchange.before() }
    verify {
      exchange.header("Access-Control-Allow-Origin", "my.origin")
      exchange.header("Access-Control-Allow-Credentials", "true")
    }
  }

  @Test fun `allow only specific origin`() = runTest {
    val cors = CorsHandler(allowedOrigins = setOf("my.origin"))
    every { exchange.header("Origin") } returns "my.origin"
    cors.run {exchange.before() }
    verify { exchange.header("Access-Control-Allow-Origin", "my.origin") }

    every { exchange.header("Origin") } returns "other.origin"
    assertThrows<ForbiddenException> { cors.run { exchange.before() } }
  }

  @Test fun `preflight request`() = runTest {
    every { exchange.method } returns OPTIONS
    every { exchange.header("Origin") } returns "my.origin"
    every { exchange.header("Access-Control-Request-Method") } returns POST.toString()
    every { exchange.header("Access-Control-Request-Headers") } returns "Custom-Header"

    cors.run { exchange.before() }

    verify {
      exchange.header("Access-Control-Allow-Methods", cors.allowedMethods.joinToString())
      exchange.header("Access-Control-Allow-Headers", "Custom-Header")
      exchange.header("Access-Control-Max-Age", cors.maxAge.inWholeSeconds.toString())
      exchange.send(OK)
    }
  }
}

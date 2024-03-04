package klite

import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.RequestMethod.GET
import klite.handlers.AssetsHandler
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AssetsHandlerTest {
  val handler = AssetsHandler(Path.of("../sample/public"), useIndexForUnknownPaths = true)
  val exchange = mockk<HttpExchange>(relaxed = true) {
    every { method } returns GET
  }

  @Test fun `forbidden if trying to access paths outside of root`() {
    every { exchange.path } returns "/../README.md"
    expect { runBlocking { handler.invoke(exchange) } }.toThrow<ForbiddenException>()
  }

  @Test fun `index must be revalidated`() {
    every { exchange.path } returns "/"
    runBlocking { handler.invoke(exchange) }
    verify { exchange.responseHeaders += handler.indexHeaders }
  }

  @Test fun `sub-index must be revalidated`() {
    every { exchange.path } returns "/spa/path"
    runBlocking { handler.invoke(exchange) }
    verify { exchange.responseHeaders += handler.indexHeaders }
  }

  @Test fun `resources can be cached for longer`() {
    every { exchange.path } returns "/favicon.ico"
    runBlocking { handler.invoke(exchange) }
    exchange.responseHeaders.also {
      verify { it += handler.additionalHeaders }
      verify(exactly = 0) { it += handler.indexHeaders }
    }
  }
}

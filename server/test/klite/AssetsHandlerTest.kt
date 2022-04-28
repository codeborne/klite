package klite

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AssetsHandlerTest {
  val handler = AssetsHandler(Path.of("../sample/public"), useIndexForUnknownPaths = true)
  val exchange = mockk<HttpExchange>(relaxed = true)

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

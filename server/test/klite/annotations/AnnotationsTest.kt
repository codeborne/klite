package klite.annotations

import io.mockk.every
import io.mockk.mockk
import klite.HttpExchange
import klite.Server
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnnotationsTest {
  val exchange = mockk<HttpExchange>()
  @Path("/context") class Routes {
    @GET fun root() = "Hello"
    @GET("/hello/:world") fun params(e: HttpExchange, @PathParam world: String, @QueryParam date: String) = "Hello $world $date"
  }

  @Test
  fun `annotated instance`() {
    Server().annotated(Routes())
  }

  @Test
  fun `annotated class`() {
    Server().annotated<Routes>()
  }

  @Test
  fun `no parameter handler`() {
    val handler = toHandler(Routes(), Routes::root)
    runBlocking { assertThat(handler(exchange)).isEqualTo("Hello") }
  }

  @Test
  fun `exchange parameter handler`() {
    val handler = toHandler(Routes(), Routes::params)
    every { exchange.path("world") } returns "World"
    every { exchange.query("date") } returns "2021-10-21"
    runBlocking { assertThat(handler(exchange)).isEqualTo("Hello World 2021-10-21") }
  }
}

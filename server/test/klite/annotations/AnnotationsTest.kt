package klite.annotations

import io.mockk.every
import io.mockk.mockk
import klite.HttpExchange
import klite.Server
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AnnotationsTest {
  val converter = TypeConverter()
  val exchange = mockk<HttpExchange>()
  @Path("/context") class Routes {
    @GET fun root() = "Hello"
    @GET("/hello/:world") fun params(e: HttpExchange, @PathParam world: BigDecimal, @QueryParam date: LocalDate) = "Hello $world $date"
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
    val handler = toHandler(Routes(), Routes::root, converter)
    runBlocking { assertThat(handler(exchange)).isEqualTo("Hello") }
  }

  @Test
  fun `exchange parameter handler`() {
    val handler = toHandler(Routes(), Routes::params, converter)
    every { exchange.path("world") } returns "7.9e9"
    every { exchange.query("date") } returns "2021-10-21"
    runBlocking { assertThat(handler(exchange)).isEqualTo("Hello 7.9E+9 2021-10-21") }
  }
}

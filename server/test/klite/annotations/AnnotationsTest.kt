package klite.annotations

import io.mockk.*
import klite.HttpExchange
import klite.PathParamRegexer
import klite.Router
import klite.require
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*

class AnnotationsTest {
  val exchange = mockk<HttpExchange>()
  @Path("/context") class Routes {
    @GET fun root() = "Hello"

    @GET("/hello/:world")
    fun generic(e: HttpExchange, body: String, @PathParam world: BigDecimal, @QueryParam date: LocalDate,
                @HeaderParam header: Long, @CookieParam cookie: Locale, @AttrParam attr: BigInteger
    ) = "Hello $body $world $date $header $cookie $attr"

    @GET("/hello/specific") fun specific() = "Hello"
  }
  val router = mockk<Router>(relaxed = true)

  @Test
  fun `annotated instance`() {
    router.annotated(Routes())
    verifyOrder {
      router.add(match { it.annotations.containsAll(Routes::root.annotations) })
      router.add(match { it.annotations.containsAll(Routes::specific.annotations) })
      router.add(match { it.annotations.containsAll(Routes::generic.annotations) })
    }
  }

  @Test
  fun `annotated class`() {
    every { router.require<Routes>() } returns Routes()
    router.annotated<Routes>()
    verify(exactly = 3) { router.add(any()) }
  }

  @Test
  fun `no parameter handler`() {
    val handler = toHandler(Routes(), Routes::root)
    runBlocking { assertThat(handler(exchange)).isEqualTo("Hello") }
  }

  @Test
  fun `exchange parameter handler`() {
    val handler = toHandler(Routes(), Routes::generic)
    every { exchange.body<String>() } returns "TheBody"
    every { exchange.path("world") } returns "7.9e9"
    every { exchange.query("date") } returns "2021-10-21"
    every { exchange.header("header") } returns "42"
    every { exchange.cookie("cookie") } returns "et"
    every { exchange.attr<BigInteger>("attr") } returns BigInteger("90909")
    runBlocking { assertThat(handler(exchange)).isEqualTo("Hello TheBody 7.9E+9 2021-10-21 42 et 90909") }
  }
}

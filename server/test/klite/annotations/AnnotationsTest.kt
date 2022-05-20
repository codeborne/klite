package klite.annotations

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.*
import klite.HttpExchange
import klite.PathParamRegexer
import klite.Router
import klite.require
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*

annotation class CustomAnnotation(val hello: String)

class AnnotationsTest {
  val exchange = mockk<HttpExchange>()
  @Path("/context") @CustomAnnotation("class") class Routes {
    @GET fun root() = "Hello"

    @GET("/hello/:world")
    fun generic(body: String, @PathParam world: BigDecimal, @QueryParam date: LocalDate, @HeaderParam header: Long,
      @CookieParam cookie: Locale, @AttrParam attr: BigInteger
    ) = "Hello $body $world $date $header $cookie $attr"

    @GET("/hello/specific") @CustomAnnotation("method") fun specific() = "Hello"

    @GET("/hello/inputstream") fun stream(body: InputStream) = "Hello"
  }
  val router = spyk(Router("", mockk(), PathParamRegexer(), emptyList(), emptyList(), emptyList()))

  @Test fun `annotated instance`() {
    router.annotated(Routes())
    verifyOrder {
      router.add(match { it.annotations.containsAll(Routes::root.annotations) })
      router.add(match { it.annotations.containsAll(Routes::stream.annotations) })
      router.add(match { it.annotations.containsAll(Routes::specific.annotations) })
      router.add(match { it.annotations.containsAll(Routes::generic.annotations) })
    }
  }

  @Test fun `method annotations win class annotations`() {
    router.annotated(Routes())
    verify {
      router.add(match { it.path.toString() == "/context" && it.annotation<CustomAnnotation>()!!.hello == "class" })
      router.add(match { it.path.toString() == "/context/hello/specific" && it.annotation<CustomAnnotation>()!!.hello == "method" })
    }
  }

  @Test fun `annotated class`() {
    every { router.require<Routes>() } returns Routes()
    router.annotated<Routes>()
    verify(exactly = 4) { router.add(any()) }
  }

  @Test fun `no parameter handler`() {
    val handler = toHandler(Routes(), Routes::root)
    runBlocking { expect(handler(exchange)).toEqual("Hello") }
  }

  @Test fun `exchange parameter handler`() {
    val handler = toHandler(Routes(), Routes::generic)
    every { exchange.body<String>() } returns "TheBody"
    every { exchange.path("world") } returns "7.9e9"
    every { exchange.query("date") } returns "2021-10-21"
    every { exchange.header("header") } returns "42"
    every { exchange.cookie("cookie") } returns "et"
    every { exchange.attr<BigInteger>("attr") } returns BigInteger("90909")
    runBlocking { expect(handler(exchange)).toEqual("Hello TheBody 7.9E+9 2021-10-21 42 et 90909") }
  }
}

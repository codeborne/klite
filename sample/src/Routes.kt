import kotlinx.coroutines.delay
import klite.HttpExchange
import klite.annotations.GET
import klite.annotations.Path

@Path("/api")
class Routes {
  @GET("/hello")
  fun sayHello() = "Hello"

  @GET("/hello2")
  fun withExchange(exchange: HttpExchange) = "Hello2 ${exchange.method} ${exchange.path}"

  @GET("/hello3")
  fun HttpExchange.asContext() = "Hello3 $method $path"

  @GET("/suspend")
  suspend fun suspend(exchange: HttpExchange) {
    delay(100)
    "Suspend ${exchange.method} ${exchange.path}"
  }
}

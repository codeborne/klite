import klite.HttpExchange
import klite.annotations.GET
import klite.annotations.Path
import kotlinx.coroutines.delay

@Path("/hello")
class Routes {
  @GET
  fun sayHello() = "Hello"

  @GET("2")
  fun withExchange(exchange: HttpExchange) = "Hello2 ${exchange.method} ${exchange.path}"

  @GET("3")
  fun HttpExchange.asContext() = "Hello3 $method $path"

  @GET("/suspend")
  suspend fun suspend(exchange: HttpExchange) {
    delay(100)
    "Suspend ${exchange.method} ${exchange.path}"
  }
}

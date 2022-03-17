import klite.HttpExchange
import klite.annotations.GET
import klite.annotations.Path
import kotlinx.coroutines.delay

@Path("/hello")
class Routes {
  @GET
  fun sayHello() = SomeResponse("Hello")

  @GET("2")
  fun withExchange(exchange: HttpExchange) = "Hello2 ${exchange.method} ${exchange.path}"

  @GET("3")
  fun HttpExchange.asContext() = "Hello3 $method $path"

  @GET("/suspend")
  suspend fun suspend(exchange: HttpExchange): String {
    delay(100)
    return "Suspend ${exchange.method} ${exchange.path}"
  }

  @GET("/suspend204")
  suspend fun suspendNoContent() {
    delay(50)
  }

  @GET("/null")
  fun returnNull() = null

  @GET("/admin") @AdminOnly
  fun onlyForAdmins() = "Only for admins"
}

data class SomeResponse(val hello: String, val world: Double = Math.PI)

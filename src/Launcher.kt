import com.sun.net.httpserver.HttpExchange
import kotlinx.coroutines.delay
import server.AssetsHandler
import server.GET
import server.Server
import server.routesFrom
import java.nio.file.Path

// run with --illegal-access=permit to allow accessing Java built-in Mime types
fun main() {
  Server(8080).apply {
    assets("/", AssetsHandler(Path.of("public")))
    context("/hello") {
      get { "Hello World" }
      get("/delay") {
        delay(1000)
        "Waited for 1 sec"
      }
    }
    context("/failure") {
      get { error("Failure") }
    }
    routesFrom(Routes())
    start()
  }
}

@server.Path("/api")
class Routes {
  @GET("/hello")
  fun sayHello() = "Hello"

  @GET("/hello2")
  fun withExchange(exchange: HttpExchange) = "Hello ${exchange.requestMethod} ${exchange.requestURI}"

  @GET("/suspend")
  suspend fun suspend(exchange: HttpExchange) {
    delay(100)
    "Suspend ${exchange.requestMethod} ${exchange.requestURI}"
  }
}

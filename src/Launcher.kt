import kotlinx.coroutines.delay
import server.AssetsHandler
import server.HttpExchange
import server.Server
import server.annotations.GET
import server.annotations.annotated
import java.lang.Thread.currentThread
import java.nio.file.Path

// run with --illegal-access=permit to allow accessing Java built-in Mime types
fun main() {
  System.setProperty("java.util.logging.config.file", currentThread().contextClassLoader.getResource("logging.properties").file)

  Server(8080).apply {
    assets("/", AssetsHandler(Path.of("public")))
    context("/hello") {
      get { "Hello World" }
      get("/delay") {
        delay(1000)
        "Waited for 1 sec"
      }
      get("/:param") {
        "Path: ${path("param")}, Query: $queryParams"
      }
    }
    context("/failure") {
      get { error("Failure") }
    }
    annotated(Routes())
    start()
  }
}

@server.annotations.Path("/api")
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

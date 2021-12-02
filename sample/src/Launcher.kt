import kotlinx.coroutines.delay
import server.AssetsHandler
import server.Server
import server.annotations.annotated
import java.nio.file.Path

// run with --illegal-access=permit to allow accessing Java built-in Mime types
fun main() {
  System.setProperty("java.util.logging.config.file", "logging.properties")

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

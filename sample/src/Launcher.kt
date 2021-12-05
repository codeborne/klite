import klite.AssetsHandler
import klite.Server
import klite.annotations.annotated
import kotlinx.coroutines.delay
import java.nio.file.Path

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
    annotated<Routes>()
    start()
  }
}

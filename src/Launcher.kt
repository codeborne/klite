import kotlinx.coroutines.delay
import server.AssetsHandler
import server.Server
import java.nio.file.Path

// run with --illegal-access=permit to allow accessing Java built-in Mime types
fun main() {
  Server(8080).apply {
    assets("/", AssetsHandler(Path.of("public")))
    route("/") { "Hello World" }
    route("/delay") {
      delay(1000)
      "Waited for 1 sec"
    }
    route("/failure") {
      error("Failure")
    }
    start()
  }
}

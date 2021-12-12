import klite.AssetsHandler
import klite.Config
import klite.Server
import klite.annotations.annotated
import klite.jdbc.DBModule
import klite.jdbc.RequestTransactionHandler
import klite.json.enableJson
import kotlinx.coroutines.delay
import java.nio.file.Path

fun main() {
  Config.fromEnvFile()

  Server().apply {
    use(DBModule())
    use(RequestTransactionHandler())

    assets("/", AssetsHandler(Path.of("public")))

    context("/hello") {
      get { "Hello World" }
      get("/delay") {
        delay(1000)
        "Waited for 1 sec"
      }
      get("/failure") { error("Failure") }
      get("/:param") {
        "Path: ${path("param")}, Query: $queryParams"
      }
    }

    context("/api") {
      enableJson()
      annotated<Routes>()
    }

    start()
  }
}

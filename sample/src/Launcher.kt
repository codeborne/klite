import klite.AssetsHandler
import klite.Config
import klite.Server
import klite.annotations.annotated
import klite.jdbc.DBModule
import klite.jdbc.RequestTransactionHandler
import klite.json.JsonBody
import klite.require
import kotlinx.coroutines.delay
import java.nio.file.Path

fun main() {
  Config.useEnvFile()

  Server().apply {
    use(JsonBody())
    use(DBModule())
    use(RequestTransactionHandler())

    assets("/", AssetsHandler(Path.of("public")))

    before(require<AdminChecker>())

    context("/hello") {
      get { "Hello World" }

      get("/delay") {
        delay(1000)
        "Waited for 1 sec"
      }

      get("/failure") { error("Failure") }

      get("/admin") @AdminOnly {
        "Only for admins" // TODO: Kotlin bug: suspend lambda annotations don't work yet
      }

      get("/:param") {
        "Path: ${path("param")}, Query: $queryParams"
      }
    }

    context("/api") {
      useOnly<JsonBody>()
      annotated<Routes>()
    }

    start()
  }
}

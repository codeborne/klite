import klite.*
import klite.annotations.annotated
import klite.jdbc.DBModule
import klite.jdbc.RequestTransactionHandler
import klite.json.JsonBody
import kotlinx.coroutines.delay
import java.nio.file.Path

fun main() {
  Config.useEnvFile()

  Server().apply {
    use<JsonBody>() // enables parsing/sending of application/json requests/responses
    use<DBModule>() // configures a DataSource
    use<RequestTransactionHandler>() // runs each request in a transaction

    // if you need to parse x-www-form-urlencoded POST parameters as @BodyParam's
    parsers += FormUrlEncodedParser()

    assets("/", AssetsHandler(Path.of("public"), useIndexForUnknownPaths = true))

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

      post("/post") {
        data class JsonRequest(val required: String, val hello: String = "World")
        body<JsonRequest>()
      }
    }

    context("/api") {
      useOnly<JsonBody>()
      annotated<Routes>()
    }

    start()
  }
}

import klite.*
import klite.annotations.annotated
import klite.jdbc.*
import klite.json.JsonBody
import kotlinx.coroutines.delay
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.nio.file.Path
import java.time.Duration.ofSeconds

fun main() {
  sampleServer().start()
}

fun sampleServer(port: Int = 8080) = Server(listen = InetSocketAddress(port)).apply {
  Config.useEnvFile()
  use<JsonBody>() // enables parsing/sending of application/json requests/responses, depending on the Accept header

  if (Config.isDev) startDevDB() // start docker-compose db automatically
  use(DBModule(PooledDataSource())) // configure a DataSource
  use<DBMigrator>() //  migrate the DB
  use<RequestTransactionHandler>() // runs each request in a transaction

  assets("/", AssetsHandler(Path.of("public"), useIndexForUnknownPaths = true))

  register(HttpClient.newBuilder().connectTimeout(ofSeconds(5)).build())

  before<AdminChecker>()
  after { ex, err -> ex.header("X-Error", err?.message ?: "none") }

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

    get("/param/:param") {
      "Path: ${path("param")}, Query: $queryParams"
    }

    post("/post") {
      data class JsonRequest(val required: String, val hello: String = "World")
      body<JsonRequest>()
    }

    decorator { ex, h -> "<${h(ex)}>" }
    get("/decorated") { "!!!" }
  }

  context("/api") {
    useOnly<JsonBody>() // in case only json should be supported in this context
    useHashCodeAsETag() // automatically send 304 NotModified if request generates the same response as before
    annotated<MyRoutes>() // read routes from an annotated class - such classes are easier to unit-test
  }
}

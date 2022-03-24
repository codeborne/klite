package klite.jdbc

import Routes
import SomeResponse
import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import klite.Server
import klite.annotations.annotated
import klite.json.JsonBody
import klite.json.JsonHttpClient
import klite.register
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.http.HttpClient
import java.time.Duration.ofSeconds

class ServerIntegrationTest {
  @Test fun requests() {
    val port = (Math.random() * 60000 + 1000).toInt()
    val server = Server(port = port).apply {
      registry.register(HttpClient.newBuilder().connectTimeout(ofSeconds(5)).build())
      use(JsonBody())
      context("/") {
        get("hello") { "Hello" }
      }
      context("/api") {
        useOnly<JsonBody>()
        annotated<Routes>()
      }
      start(gracefulStopDelaySec = -1)
    }

    val http = JsonHttpClient(server.registry, "http://localhost:$port")
    runBlocking {
      expect(http.get<String>("/hello")).toEqual("\"Hello\"")
      expect(http.get<SomeResponse>("/api/hello")).toEqual(SomeResponse("Hello"))
      expect(http.get<Unit>("/api/hello/suspend204")).toEqual(Unit)
      expect(http.get<String>("/api/hello/null")).toEqual("null")
      expect(http.get<String>("/api/hello/params?required=123")).toEqual("\"false,123,null\"")
    }
    expect { runBlocking { http.get<String>("/api/hello/params") } }.toThrow<IOException>()
      .messageToContain("""{"statusCode":400,"message":"required is required","reason":"Bad Request"}""")
    server.stop()
  }
}

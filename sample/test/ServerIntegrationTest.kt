package klite.sample

import MyData
import MyRoutes
import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import klite.Config
import klite.Server
import klite.annotations.annotated
import klite.jdbc.DBModule
import klite.json.JsonBody
import klite.json.JsonHttpClient
import klite.register
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.net.http.HttpRequest.BodyPublishers.noBody
import java.time.Duration.ofSeconds

class ServerIntegrationTest {
  @Test fun requests() {
    val port = (Math.random() * 60000 + 1000).toInt()
    val server = Server(listen = InetSocketAddress(port)).apply {
      Config.useEnvFile()
      use(DBModule())
      registry.register(HttpClient.newBuilder().connectTimeout(ofSeconds(5)).build())
      use(JsonBody())
      context("/") {
        get("hello") { "Hello" }
      }
      context("/api") {
        useOnly<JsonBody>()
        annotated<MyRoutes>()
      }
      start(gracefulStopDelaySec = -1)
    }

    val http = JsonHttpClient("http://localhost:$port", registry = server.registry)
    runBlocking {
      expect(http.get<String>("/hello")).toEqual("\"Hello\"")
      expect(http.get<MyData>("/api/hello")).toEqual(MyData("Hello"))
      expect(http.request<Unit>("/api/hello") { method("HEAD", noBody()) }).toEqual(Unit)
      expect(http.get<Unit>("/api/hello/suspend204")).toEqual(Unit)
      expect(http.post<Unit>("/api/hello/201", null)).toEqual(Unit)
      expect(http.get<String>("/api/hello/broken-render")).toEqual("{")
      expect(http.get<String>("/api/hello/null")).toEqual("null")
      expect(http.get<String>("/api/hello/params?required=123")).toEqual("\"false,123,null\"")
      expect(http.post<String>("/api/hello/post", MyData("World"))).toEqual("\"Received MyData(hello=World, world=3.141592653589793) as json, optional = true\"")
    }
    expect { runBlocking { http.get<String>("/api/hello/params") } }.toThrow<IOException>()
      .messageToContain("""{"statusCode":400,"message":"required is required","reason":"Bad Request"}""")

    server.stop()
  }
}

package klite.sample

import MyData
import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import klite.json.JsonHttpClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import sampleServer
import java.io.IOException
import java.net.http.HttpRequest.BodyPublishers.noBody

class ServerIntegrationTest {
  @Test fun requests() {
    val port = (Math.random() * 60000 + 1000).toInt()
    val server = sampleServer(port).apply { start(gracefulStopDelaySec = -1) }

    val http = JsonHttpClient("http://localhost:$port", registry = server.registry)
    runBlocking {
      expect(http.get<String>("/hello")).toEqual("\"Hello World\"")
      expect(http.get<String>("/hello/param/123456?query=123")).toEqual("\"Path: 123456, Query: {query=123}\"")
      expect(http.get<String>("/hello/decorated")).toEqual("\"<!!!>\"")

      expect(http.get<MyData>("/api/hello")).toEqual(MyData("Hello"))
      expect(http.request<Unit>("/api/hello") { method("HEAD", noBody()) }).toEqual(Unit)
      expect(http.get<Unit>("/api/hello/suspend204")).toEqual(Unit)
      expect(http.post<Unit>("/api/hello/201", null)).toEqual(Unit)
      expect(http.get<String>("/api/hello/broken-render")).toEqual("")
      expect(http.get<String>("/api/hello/null")).toEqual("null")
      expect(http.get<String>("/api/hello/params?required=123")).toEqual("\"false,123,null\"")
      expect(http.post<String>("/api/hello/post", MyData("World"))).toEqual("\"Received MyData(hello=World, world=3.141592653589793) as json, optional = true\"")
    }
    expect { runBlocking { http.get<String>("/api/hello/params") } }.toThrow<IOException>()
      .messageToContain("""{"message":"required is required","reason":"Bad Request","statusCode":400}""")

    server.stop()
  }
}

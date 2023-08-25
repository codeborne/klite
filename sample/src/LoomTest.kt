import com.sun.net.httpserver.HttpServer
import klite.Config
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.concurrent.Executors

fun main() {
  Config.useEnvFile()
//  val db = PooledDataSource(maxSize = 100)
  val url = URI("https://www.google.com/")
  val httpClient = HttpClient.newHttpClient()
  HttpServer.create(InetSocketAddress(8080), 10).apply {
    executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("req-", 1).factory())
    createContext("/hello") { e ->
//      val dbTime = db.query("select pg_sleep(1), clock_timestamp()") { getString(2) }.first()
//      val body = url.openStream().use { it.readAllBytes() }
      val body = httpClient.send(HttpRequest.newBuilder(url).GET().build(), BodyHandlers.ofString()).body()
      e.sendResponseHeaders(200, 0)
      e.responseBody.writer().use { it.write("${body.length}, ${Thread.currentThread().name}\n") }
    }
    start()
  }
}

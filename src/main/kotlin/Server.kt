import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.util.concurrent.Executors

fun main() {
  val dispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()
  val requestScope = CoroutineScope(SupervisorJob() + dispatcher)
  val server = HttpServer.create(InetSocketAddress(8080), 0)
  server.createContext("/") { exchange ->
    requestScope.launch {
      delay(100)
      val response = "Hello World"
      exchange.responseHeaders["Content-Type"] = listOf("text/plain")
      exchange.sendResponseHeaders(200, response.length.toLong())
      exchange.responseBody.write(response.toByteArray())
      exchange.close()
    }
  }.filters
  server.executor = null // receive requests on the main thread
  server.start()
}

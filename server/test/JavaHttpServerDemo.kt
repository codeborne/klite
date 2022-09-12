import com.sun.net.httpserver.HttpServer
import java.lang.Thread.currentThread
import java.net.InetSocketAddress

/**
 * Test it with:
 * ab -n 10000 -c 100 http://localhost:8080/hello
 */
fun main() {
  val server = HttpServer.create(InetSocketAddress(8080), 0)
  // server.executor = Executors.newFixedThreadPool(10)
  server.createContext("/hello") {
    // Thread.sleep(100)
    it.sendResponseHeaders(200, 0)
    it.responseBody.write(currentThread().name.toByteArray())
    it.close()
  }
  server.start()

  // Same with Klite
//  Server().apply {
//    context("/hello") {
//      get { currentThread().name }
//    }
//    start()
//  }
}

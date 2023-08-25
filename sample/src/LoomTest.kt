import com.sun.net.httpserver.HttpServer
import klite.Config
import klite.jdbc.PooledDataSource
import klite.jdbc.query
import java.net.InetSocketAddress
import java.util.concurrent.Executors

fun main() {
  Config.useEnvFile()
  val db = PooledDataSource(maxSize = 100)
  HttpServer.create(InetSocketAddress(8080), 10).apply {
    executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("req-", 1).factory())
    createContext("/hello") { e ->
      val dbTime = db.query("select pg_sleep(1), clock_timestamp()") { getString(2) }.first()
      e.sendResponseHeaders(200, 0)
      e.responseBody.writer().use { it.write("$dbTime, ${Thread.currentThread().name} ${Thread.currentCarrierThread().name}\n") }
    }
    start()
  }
}

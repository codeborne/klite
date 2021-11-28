import kotlinx.coroutines.delay
import server.Server

fun main() {
  Server(8080).apply {
    route("/") { "Hello World" }
    route("/delay") {
      delay(1000)
      "Waited for 1 sec"
    }
    route("/failure") {
      error("Failure")
    }
    start()
  }
}

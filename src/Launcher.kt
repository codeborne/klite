import server.Server

fun main() {
  Server(8080).apply {
    route("/") { "Hello World" }
    start()
  }
}

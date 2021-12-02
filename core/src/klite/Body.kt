package klite

interface BodyRenderer {
  fun render(exchange: HttpExchange, value: Any?)
}

class TextBodyRenderer(val contentType: String = "text/plain"): BodyRenderer {
  override fun render(exchange: HttpExchange, value: Any?) {
    exchange.send(200, value, contentType)
  }
}

interface BodyParser {
  fun parse(exchange: HttpExchange, contentType: String)
}

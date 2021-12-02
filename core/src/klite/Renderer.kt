package klite

typealias ContentRenderer = (exchange: HttpExchange, result: Any?) -> Unit

class TextContentRenderer(val contentType: String = "text/plain"): ContentRenderer {
  override fun invoke(exchange: HttpExchange, result: Any?) {
    exchange.send(200, result, contentType)
  }
}

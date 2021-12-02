package klite

typealias ResultRenderer = (exchange: HttpExchange, result: Any?) -> Unit

class TextResultRenderer(val defaultContentType: String = "text/plain"): ResultRenderer {
  override fun invoke(exchange: HttpExchange, result: Any?) {
    exchange.send(200, result, defaultContentType)
  }
}

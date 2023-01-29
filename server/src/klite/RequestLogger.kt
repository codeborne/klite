package klite

typealias RequestLogFormatter = HttpExchange.(ms: Long) -> String?
val defaultRequestLogFormatter: RequestLogFormatter = { ms -> "$remoteAddress $method $path$query: $statusCode in $ms ms - $browser" + (failure?.let { " - $it" } ?: "")}

open class RequestLogger(
  val formatter: RequestLogFormatter = defaultRequestLogFormatter
): Decorator {
  private val logger = logger()

  override suspend fun invoke(exchange: HttpExchange, handler: Handler): Any? {
    val start = System.nanoTime()
    exchange.onComplete {
      val ms = (System.nanoTime() - start) / 1000_000
      formatter(exchange, ms)?.let { logger.info(it) }
    }
    return handler(exchange)
  }
}

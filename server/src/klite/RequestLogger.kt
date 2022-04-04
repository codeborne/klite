package klite

typealias RequestLogFormatter = HttpExchange.(ms: Long) -> String?

open class RequestLogger(
  val formatter: RequestLogFormatter = { ms -> "$remoteAddress $method $path$query: $statusCode in $ms ms" }
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

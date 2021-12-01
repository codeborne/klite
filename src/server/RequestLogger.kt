package server

typealias RequestLogFormatter = HttpExchange.(ms: Long) -> String

class RequestLogger(
  val formatter: RequestLogFormatter = { ms -> "$remoteAddress $method $path$query: $statusCode in $ms ms" }
): Decorator {
  private val logger = System.getLogger(javaClass.name)

  override suspend fun invoke(exchange: HttpExchange, handler: Handler): Any? {
    val start = System.nanoTime()
    // TODO: unique request id from X-Request-Id or generated
    try { return handler(exchange) }
    finally {
      val ms = (System.nanoTime() - start) / 1000_000
      logger.info(formatter(exchange, ms))
    }
  }
}

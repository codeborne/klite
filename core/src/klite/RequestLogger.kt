package klite

typealias RequestLogFormatter = HttpExchange.(ms: Long) -> String

class RequestLogger(
  val formatter: RequestLogFormatter = { ms -> "$remoteAddress $method $path$query: $statusCode in $ms ms" }
): Before {
  private val logger = System.getLogger(javaClass.name)

  override suspend fun before(exchange: HttpExchange) {
    val start = System.nanoTime()
    // TODO: unique request id from X-Request-Id or generated
    exchange.onComplete {
      val ms = (System.nanoTime() - start) / 1000_000
      logger.info(formatter(exchange, ms))
    }
  }
}

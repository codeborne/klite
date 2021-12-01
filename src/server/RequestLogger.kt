package server

import java.util.logging.Logger

class RequestLogger: AsyncFilter {
  private val log = Logger.getLogger(javaClass.name)

  override fun before(exchange: HttpExchange) {
    exchange.attr("start", System.nanoTime())
  }

  override fun after(exchange: HttpExchange, e: Throwable?) {
    exchange.apply {
      val ms = (System.nanoTime() - attr<Long>("start")) / 1000_000
      log.info("$remoteAddress $method $path$query: $responseCode in $ms ms")
    }
  }
}

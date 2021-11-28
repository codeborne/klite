package server

import com.sun.net.httpserver.HttpExchange
import java.util.logging.Logger

class RequestLogger: Filter() {
  private val log = Logger.getLogger(javaClass.name)

  override fun doFilter(exchange: HttpExchange, chain: Chain) {
    val start = System.nanoTime()
    chain.doFilter(exchange)
    exchange.apply {
      val ms = (System.nanoTime() - start) / 1000_000
      log.info("$requestMethod $requestURI: $responseCode in $ms ms")
    }
  }
}
package server

import com.sun.net.httpserver.Filter

abstract class AsyncFilter: Filter() {
  override fun description() = javaClass.simpleName

  open fun before(exchange: HttpExchange) {}
  open fun after(exchange: HttpExchange, e: Throwable?) {}

  override fun doFilter(exchange: HttpExchange, chain: Chain) {
    before(exchange)
    chain.doFilter(exchange)
  }
}

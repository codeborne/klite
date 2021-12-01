package server

interface AsyncFilter {
  fun before(exchange: HttpExchange) {}
  fun after(exchange: HttpExchange, e: Throwable?) {}
}

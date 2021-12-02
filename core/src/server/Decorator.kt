package server

typealias Handler = suspend HttpExchange.() -> Any?
typealias Decorator = suspend (exchange: HttpExchange, handler: Handler) -> Any?

internal fun Decorator.wrap(handler: Handler): Handler = { invoke(this, handler) }
internal fun List<Decorator>.wrap(handler: Handler) = foldRight(handler) { d, h -> d.wrap(h) }

interface Before {
  suspend fun before(exchange: HttpExchange)
}

internal fun Before.toDecorator(): Decorator = { ex, next ->
  before(ex); next(ex)
}

interface After {
  suspend fun after(exchange: HttpExchange, exception: Exception?)
}

internal fun After.toDecorator(): Decorator = { ex, next ->
  try { next(ex); after(ex, null) }
  catch (e: Exception) { after(ex, e) }
}

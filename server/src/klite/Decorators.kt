package klite

typealias Handler = suspend HttpExchange.() -> Any?
typealias Decorator = suspend (exchange: HttpExchange, handler: Handler) -> Any?

fun Decorator.wrap(handler: Handler): Handler = { invoke(this, handler) }
fun List<Decorator>.wrap(handler: Handler) = foldRight(handler) { d, h -> d.wrap(h) }

fun interface Before {
  suspend fun before(exchange: HttpExchange)
}

fun Before.toDecorator(): Decorator = { ex, next ->
  before(ex); next(ex)
}

fun interface After {
  suspend fun after(exchange: HttpExchange, error: Throwable?)
}

fun After.toDecorator(): Decorator = { ex, next ->
  var error: Throwable? = null
  try { next(ex) }
  catch (e: Throwable) { error = e; throw e }
  finally { after(ex, error) }
}

fun Registry.requireAllDecorators() =
  requireAll<Before>().map { it.toDecorator() } + requireAll() + requireAll<After>().map { it.toDecorator() }

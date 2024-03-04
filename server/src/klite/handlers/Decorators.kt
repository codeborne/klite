package klite.handlers

import klite.HttpExchange
import klite.Registry
import klite.requireAll

typealias Handler = suspend HttpExchange.() -> Any?
typealias Decorator = suspend (exchange: HttpExchange, handler: Handler) -> Any?

fun Decorator.wrap(handler: Handler): Handler = { invoke(this, handler) }
fun List<Decorator>.wrap(handler: Handler) = foldRight(handler) { d, h -> d.wrap(h) }

fun interface Before {
  suspend fun HttpExchange.before()
}

fun Before.toDecorator(): Decorator = { ex, handler ->
  ex.before(); ex.handler()
}

fun interface After {
  suspend fun HttpExchange.after(error: Throwable?)
}

fun After.toDecorator(): Decorator = { ex, handler ->
  var error: Throwable? = null
  try { handler(ex) }
  catch (e: Throwable) { error = e; throw e }
  finally { ex.after(error) }
}

fun Registry.requireAllDecorators() =
  requireAll<Before>().map { it.toDecorator() } + requireAll() + requireAll<After>().map { it.toDecorator() }

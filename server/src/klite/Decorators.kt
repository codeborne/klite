package klite

typealias Handler = suspend HttpExchange.() -> Any?
typealias Decorator = suspend (exchange: HttpExchange, handler: Handler) -> Any?

fun Decorator.wrap(handler: Handler): Handler = { invoke(this, handler) }
fun List<Decorator>.wrap(handler: Handler) = foldRight(handler) { d, h -> d.wrap(h) }

fun interface Before {
  suspend fun before(exchange: HttpExchange)
}

fun interface After {
  suspend fun after(exchange: HttpExchange, error: Throwable?)
}

data class Decorators(
  val before: List<Before> = emptyList(),
  val around: List<Decorator> = emptyList(),
  val after: List<After> = emptyList()
) {
  operator fun plus(before: Before) = copy(before = this.before + before)
  operator fun plus(around: Decorator) = copy(around = this.around + around)
  operator fun plus(after: After) = copy(after = this.after + after)

  fun wrap(handler: Handler): Handler = around.wrap(handler).let { wrapped -> {
    before.forEach { it.before(this) }
    var error: Throwable? = null
    try { wrapped(this) }
    catch (e: Throwable) { error = e; throw e }
    finally { after.forEach { it.after(this, error) } }
  }}
}

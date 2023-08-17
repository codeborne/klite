package klite

import klite.StatusCode.Companion.BadRequest
import klite.StatusCode.Companion.InternalServerError
import klite.StatusCode.Companion.NotFound
import klite.StatusCode.Companion.UnprocessableEntity
import kotlin.reflect.KClass

open class BusinessException(messageKey: String, cause: Throwable? = null): Exception(messageKey, cause)

typealias ThrowableHandler<T> = HttpExchange.(e: T) -> ErrorResponse?

open class ErrorHandler {
  private val logger = logger(ErrorHandler::class.qualifiedName!!)
  private val handlers = mutableMapOf<KClass<out Throwable>, ThrowableHandler<Throwable>>()
  private val statusCodes = mutableMapOf<KClass<out Throwable>, StatusCode>(
    IllegalArgumentException::class to BadRequest,
    IllegalStateException::class to BadRequest,
    BusinessException::class to UnprocessableEntity
  )

  init {
    on<NoSuchElementException> { e -> logger.error(e); ErrorResponse(NotFound, e.message?.takeIf { "is empty" !in it }) }
    on<NullPointerException> { e ->
      if (e.message?.startsWith("Parameter specified as non-null is null") == true)
        ErrorResponse(BadRequest, e.message!!.substring(e.message!!.indexOf(", parameter ") + 12) + " is required")
      else null
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun <T: Throwable> on(e: KClass<out T>, handler: ThrowableHandler<T>) { handlers[e] = handler as ThrowableHandler<Throwable> }
  inline fun <reified T: Throwable> on(noinline handler: ThrowableHandler<T>) = on(T::class, handler)

  fun on(e: KClass<out Throwable>, statusCode: StatusCode) { statusCodes[e] = statusCode }

  fun handle(exchange: HttpExchange, e: Throwable) {
    exchange.failure = e
    if (!exchange.isResponseStarted) toResponse(exchange, e).let {
      if (it.statusCode.bodyAllowed) exchange.render(it.statusCode, it) else exchange.send(it.statusCode)
    } else if (e.message == null || e.message!!.let { "Broken pipe" !in it && "Connection reset" !in it })
      logger.error("Error after headers sent", e)
  }

  open fun toResponse(exchange: HttpExchange, e: Throwable): ErrorResponse {
    if (e is RedirectException) exchange.header("Location", e.location)
    if (e is StatusCodeException) return ErrorResponse(e.statusCode, e.message)

    // TODO: look for subclasses
    handlers[e::class]?.let { handler ->
      exchange.handler(e)?.let { return it }
    }
    statusCodes[e::class]?.let {
      logger.error(e)
      return ErrorResponse(it, e.message)
    }
    return unhandled(e)
  }

  open fun unhandled(e: Throwable): ErrorResponse {
    logger.error("Unhandled exception", e)
    return ErrorResponse(InternalServerError, e.message.takeUnless { Config.isProd })
  }
}

data class ErrorResponse(val statusCode: StatusCode, val message: String?) {
  val reason: String = StatusCode.reasons[statusCode] ?: ""
  override fun toString() = "${statusCode.value} $reason\n${message ?: ""}"
}

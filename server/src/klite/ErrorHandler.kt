package klite

import klite.StatusCode.Companion.BadRequest
import klite.StatusCode.Companion.InternalServerError
import klite.StatusCode.Companion.NotFound
import klite.StatusCode.Companion.UnprocessableEntity
import kotlin.reflect.KClass

open class BusinessException(messageKey: String, cause: Throwable? = null): Exception(messageKey, cause)

fun interface ThrowableHandler<in E: Throwable> {
  fun handle(e: E, exchange: HttpExchange): ErrorResponse?
}

open class ErrorHandler {
  private val logger = logger(ErrorHandler::class.qualifiedName!!)
  private val handlers = mutableMapOf<KClass<out Throwable>, ThrowableHandler<Throwable>>()
  private val statusCodes = mutableMapOf<KClass<out Throwable>, StatusCode>(
    NoSuchElementException::class to NotFound,
    IllegalArgumentException::class to BadRequest,
    IllegalStateException::class to BadRequest,
    BusinessException::class to UnprocessableEntity
  )

  @Suppress("UNCHECKED_CAST")
  fun <T: Throwable> on(e: KClass<out T>, handler: ThrowableHandler<T>) { handlers[e] = handler as ThrowableHandler<Throwable> }
  fun on(e: KClass<out Throwable>, statusCode: StatusCode) { statusCodes[e] = statusCode }

  fun handle(exchange: HttpExchange, e: Throwable) =
    if (!exchange.isResponseStarted) toResponse(exchange, e)?.let { exchange.render(it.statusCode, it) }
    else logger.error("Error after headers sent: $e")

  open fun toResponse(exchange: HttpExchange, e: Throwable): ErrorResponse? {
    if (e is StatusCodeException) return ErrorResponse(e.statusCode, e.message)
    if (e is NullPointerException && e.message?.startsWith("Parameter specified as non-null is null") == true)
      return ErrorResponse(BadRequest, e.message!!.substring(e.message!!.indexOf(", parameter ") + 12) + " is required")

    // TODO: look for subclasses
    statusCodes[e::class]?.let {
      logger.error(e)
      return ErrorResponse(it, e.message)
    }
    handlers[e::class]?.let { handler ->
      return handler.handle(e, exchange)
    }
    return unhandled(e)
  }

  open fun unhandled(e: Throwable): ErrorResponse {
    logger.error("Unhandled exception", e)
    return ErrorResponse(InternalServerError, e.message)
  }
}

data class ErrorResponse(val statusCode: StatusCode, val message: String?) {
  val reason: String = StatusCode.reasons[statusCode] ?: ""
  override fun toString() = "${statusCode.value} $reason\n${message ?: ""}"
}

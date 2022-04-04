package klite

import klite.StatusCode.Companion.BadRequest
import klite.StatusCode.Companion.InternalServerError
import klite.StatusCode.Companion.NotFound
import kotlin.reflect.KClass

fun interface ExceptionHandler<in E: Exception> {
  fun handle(e: E, exchange: HttpExchange): ErrorResponse?
}

open class ErrorHandler {
  private val logger = logger()
  private val handlers = mutableMapOf<KClass<out Exception>, ExceptionHandler<Exception>>()
  private val statusCodes = mutableMapOf<KClass<out Exception>, StatusCode>(
    NoSuchElementException::class to NotFound,
    IllegalArgumentException::class to BadRequest,
    IllegalStateException::class to BadRequest
  )

  @Suppress("UNCHECKED_CAST")
  fun <T: Exception> on(e: KClass<out T>, handler: ExceptionHandler<T>) { handlers[e] = handler as ExceptionHandler<Exception> }
  fun on(e: KClass<out Exception>, statusCode: StatusCode) { statusCodes[e] = statusCode }

  fun handle(exchange: HttpExchange, e: Exception) = toResponse(exchange, e)?.also {
    if (!exchange.isResponseStarted) exchange.render(it.statusCode, it)
    else logger.error("Error after headers sent: $it")
  }

  open fun toResponse(exchange: HttpExchange, e: Exception): ErrorResponse? {
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

  open fun unhandled(e: Exception): ErrorResponse {
    logger.error("Unhandled exception", e)
    return ErrorResponse(InternalServerError, e.message)
  }
}

data class ErrorResponse(val statusCode: StatusCode, val message: String?) {
  val reason: String = StatusCode.reasons[statusCode] ?: ""
  override fun toString() = "${statusCode.value} $reason\n${message ?: ""}"
}

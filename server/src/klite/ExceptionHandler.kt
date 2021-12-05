package klite

import klite.StatusCode.Companion.BadRequest
import klite.StatusCode.Companion.NotFound
import kotlin.reflect.KClass

typealias ExceptionHandler = (exchange: HttpExchange, e: Exception) -> ErrorResponse?

open class ErrorHandler {
  private val logger = logger()
  private val handlers = mutableMapOf<KClass<out Exception>, ExceptionHandler>()
  private val statusCodes = mutableMapOf<KClass<out Exception>, StatusCode>(
    NoSuchElementException::class to NotFound,
    IllegalArgumentException::class to BadRequest,
    IllegalStateException::class to BadRequest
  )

  fun on(e: KClass<out Exception>, handler: ExceptionHandler) { handlers[e] = handler }
  fun on(e: KClass<out Exception>, statusCode: StatusCode) { statusCodes[e] = statusCode }

  operator fun invoke(exchange: HttpExchange, e: Exception) =
    toResponse(exchange, e)?.also { exchange.render(it.statusCode, it) }

  open fun toResponse(exchange: HttpExchange, e: Exception): ErrorResponse? {
    if (e is StatusCodeException) return ErrorResponse(e.statusCode, e.message)
    // TODO: look for subclasses
    statusCodes[e::class]?.let {
      logger.log(System.Logger.Level.ERROR, e.toString(), e)
      return ErrorResponse(it, e.message)
    }
    handlers[e::class]?.let { handler ->
      return handler(exchange, e)
    }
    return unhandled(e)
  }

  open fun unhandled(e: Exception): ErrorResponse {
    logger.log(System.Logger.Level.ERROR, "Unhandled exception", e)
    return ErrorResponse(StatusCode.InternalServerError, e.message)
  }
}

data class ErrorResponse(val statusCode: StatusCode, val message: String?) {
  val reason: String = StatusCode.reasons[statusCode] ?: ""
  override fun toString() = "${statusCode.value} $reason\n${message ?: ""}"
}

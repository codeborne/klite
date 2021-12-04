package klite

import kotlin.reflect.KClass

typealias ExceptionHandler = (exchange: HttpExchange, e: Exception) -> ErrorResponse?

open class DefaultExceptionHandler: ExceptionHandler {
  private val logger = System.getLogger(javaClass.name)
  private val statusCodes = mutableMapOf<KClass<Exception>, Int>()
  private val handlers = mutableMapOf<KClass<Exception>, ExceptionHandler>()

  fun add(e: KClass<Exception>, statusCode: Int) { statusCodes[e] = statusCode }
  fun add(e: KClass<Exception>, handler: ExceptionHandler) { handlers[e] = handler }

  override fun invoke(exchange: HttpExchange, e: Exception) =
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
    return fallback(e)
  }

  open fun fallback(e: Exception): ErrorResponse {
    logger.log(System.Logger.Level.ERROR, "Unhandled exception", e)
    return ErrorResponse(500, e.message)
  }
}

data class ErrorResponse(val statusCode: Int, val message: String?) {
  val reason: String = "TODO"
  override fun toString() = "$statusCode $reason\n${message ?: ""}"
}

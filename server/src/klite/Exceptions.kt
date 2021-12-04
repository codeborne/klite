package klite

import java.lang.System.Logger.Level.ERROR
import kotlin.reflect.KClass

open class StatusCodeException(val statusCode: Int, content: String?): Exception(content)
class NotFoundException(content: String?): StatusCodeException(404, content)
class ForbiddenException(content: String?): StatusCodeException(403, content)
class UnauthorizedException(content: String?): StatusCodeException(401, content)
class UnsupportedMediaTypeException(contentType: String?): StatusCodeException(415, "Unsupported Content-Type: $contentType")
class NotAcceptableException(contentType: String?): StatusCodeException(406, "Unsupported Accept: $contentType")

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
      logger.log(ERROR, e.toString(), e)
      return ErrorResponse(it, e.message)
    }
    handlers[e::class]?.let { handler ->
      return handler(exchange, e)
    }
    return fallback(e)
  }

  open fun fallback(e: Exception): ErrorResponse {
    logger.log(ERROR, "Unhandled exception", e)
    return ErrorResponse(500, e.message)
  }
}

data class ErrorResponse(val statusCode: Int, val message: String?) {
  val reason: String = "TODO"
  override fun toString() = "$statusCode $reason\n${message ?: ""}"
}

package klite

import java.lang.System.Logger.Level.ERROR
import kotlin.reflect.KClass

open class StatusCodeException(val statusCode: Int, content: String): Exception(content)
class NotFoundException(content: String): StatusCodeException(404, content)
class ForbiddenException(content: String): StatusCodeException(403, content)
class UnauthorizedException(content: String): StatusCodeException(401, content)

typealias ExceptionHandler = (exchange: HttpExchange, e: Exception) -> Unit

open class DefaultExceptionHandler: ExceptionHandler {
  private val logger = System.getLogger(javaClass.name)
  private val statusCodes = mutableMapOf<KClass<Exception>, Int>()
  private val handlers = mutableMapOf<KClass<Exception>, ExceptionHandler>()

  fun add(e: KClass<Exception>, statusCode: Int) { statusCodes[e] = statusCode }
  fun add(e: KClass<Exception>, handler: ExceptionHandler) { handlers[e] = handler }

  override fun invoke(exchange: HttpExchange, e: Exception) {
    if (e is StatusCodeException) return exchange.send(e.statusCode, e.message)
    // TODO: look for subclasses
    statusCodes[e::class]?.let {
      logger.log(ERROR, e.toString(), e)
      return exchange.send(it, e.message)
    }
    handlers[e::class]?.let { handler ->
      return handler(exchange, e)
    }
    fallback(exchange, e)
  }

  open fun fallback(exchange: HttpExchange, e: Exception) {
    logger.log(ERROR, "Unhandled exception", e)
    if (!exchange.isResponseStarted) exchange.send(500, e.message)
  }
}

package server

import java.lang.System.Logger.Level.ERROR

open class StatusCodeException(val statusCode: Int, content: String): Exception(content)
class NotFoundException(content: String): StatusCodeException(404, content)
class ForbiddenException(content: String): StatusCodeException(403, content)
class UnauthorizedException(content: String): StatusCodeException(401, content)

open class ExceptionHandler {
  private val logger = System.getLogger(javaClass.name)

  open fun handle(exchange: HttpExchange, e: Exception) {
    when (e) {
      is StatusCodeException -> exchange.send(e.statusCode, e.message)
      else -> {
        logger.log(ERROR, "Unhandled exception", e)
        if (!exchange.isResponseStarted) exchange.send(500, e)
      }
    }
  }
}

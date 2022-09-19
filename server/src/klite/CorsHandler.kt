package klite

import klite.RequestMethod.*
import klite.StatusCode.Companion.OK

/** Currently must be registered before Server.notFoundHandler */
open class CorsHandler(
  val maxAgeSec: Int = 604800,
  val allowCredentials: Boolean = true,
  val allowedOrigins: Set<String>? = null,
  val allowedMethods: Set<RequestMethod> = setOf(GET, POST, PUT, PATCH, DELETE),
  val allowedHeaders: Set<String>? = null
): Before {
  override suspend fun before(exchange: HttpExchange) {
    val origin = exchange.header("Origin") ?: return
    if (allowedOrigins == null || allowedOrigins.contains(origin))
      exchange.header("Access-Control-Allow-Origin", origin)
    else throw ForbiddenException()

    exchange.header("Access-Control-Request-Headers")?.let {
      exchange.header("Access-Control-Allow-Headers", allowedHeaders?.joinToString() ?: it)
    }

    if (exchange.method == OPTIONS) {
      exchange.header("Access-Control-Max-Age", maxAgeSec.toString())
      if (allowCredentials) exchange.header("Access-Control-Allow-Credentials", "true")

      val requestedMethod = RequestMethod.valueOf(exchange.header("Access-Control-Request-Method")!!)
      if (!allowedMethods.contains(requestedMethod)) throw ForbiddenException()
      else exchange.header("Access-Control-Allow-Methods", allowedMethods.joinToString())

      exchange.send(OK)
    }
  }
}

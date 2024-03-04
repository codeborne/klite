package klite.handlers

import klite.ForbiddenException
import klite.HttpExchange
import klite.RequestMethod
import klite.RequestMethod.*
import klite.StatusCode.Companion.OK
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * An easy way to enable CORS (cross-origin requests).
 * Enable with `before(CorsHandler(...))`
 */
open class CorsHandler(
  val maxAge: Duration = 7.days,
  val allowCredentials: Boolean = true,
  val allowedOrigins: Set<String>? = null,
  val allowedMethods: Set<RequestMethod> = setOf(GET, POST, PUT, PATCH, DELETE),
  val allowedHeaders: Set<String>? = null
): Before {
  override suspend fun HttpExchange.before() {
    val origin = header("Origin") ?: return
    if (allowedOrigins == null || allowedOrigins.contains(origin)) {
      header("Access-Control-Allow-Origin", origin)
      if (allowCredentials) header("Access-Control-Allow-Credentials", "true")
    } else throw ForbiddenException()

    header("Access-Control-Request-Headers")?.let {
      header("Access-Control-Allow-Headers", allowedHeaders?.joinToString() ?: it)
    }

    if (method == OPTIONS) {
      header("Access-Control-Max-Age", maxAge.inWholeSeconds.toString())

      val requestedMethod = RequestMethod.valueOf(header("Access-Control-Request-Method")!!)
      if (!allowedMethods.contains(requestedMethod)) throw ForbiddenException()
      else header("Access-Control-Allow-Methods", allowedMethods.joinToString())

      send(OK)
    }
  }
}

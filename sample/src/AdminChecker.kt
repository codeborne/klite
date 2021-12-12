import klite.Before
import klite.ForbiddenException
import klite.HttpExchange
import klite.annotation

@Target(AnnotationTarget.FUNCTION)
annotation class AdminOnly

class AdminChecker: Before {
  override suspend fun before(exchange: HttpExchange) {
    if (exchange.route?.annotation<AdminOnly>() != null && exchange.header("I-Am-Admin") == null)
      throw ForbiddenException("Admin only")
  }
}

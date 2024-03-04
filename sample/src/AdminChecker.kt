import klite.ForbiddenException
import klite.HttpExchange
import klite.handlers.Before
import kotlin.reflect.full.hasAnnotation

@Target(AnnotationTarget.FUNCTION)
annotation class AdminOnly

class AdminChecker: Before {
  override suspend fun HttpExchange.before() {
    // implement whatever access logic you need in your app
    if (route.hasAnnotation<AdminOnly>() && header("I-Am-Admin") == null)
      throw ForbiddenException("Admin only")
  }
}

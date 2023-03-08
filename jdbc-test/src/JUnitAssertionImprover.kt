package klite.jdbc

import klite.logger
import klite.warn
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method

/** Improves mockk & atrium assertion error messages by locating expected and actual values one below another */
class JUnitAssertionImprover: InvocationInterceptor {
  override fun interceptTestMethod(invocation: InvocationInterceptor.Invocation<Void>, invocationContext: ReflectiveInvocationContext<Method>, extensionContext: ExtensionContext) {
    try {
      super.interceptTestMethod(invocation, invocationContext, extensionContext)
    } catch (e: AssertionError) {
      try {
        Throwable::class.java.getDeclaredField("detailMessage").apply {
          isAccessible = true // needs --add-opens=java.base/java.lang=ALL-UNNAMED
          set(e, e.message?.replace(", matcher:", "\n   matcher:")?.replace("◆ to equal:", "        ◆ to equal:"))
        }
      } catch (fail: Exception) { logger().warn("Warning: cannot modify AssertionError: $e") }
      throw e
    }
  }
}

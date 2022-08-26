package klite.slf4j

import ch.tutteli.atrium.api.fluent.en_GB.notToContain
import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.mockk
import klite.Decorator
import klite.Handler
import klite.wrap
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class StackTraceOptimizingLoggerTest {
  @Test fun `cuts stack trace until klite`() {
    val out = ByteArrayOutputStream()
    KliteLogger.out = PrintStream(out)
    val decorator: Decorator = { e, h -> h(e) }
    val handler: Handler = {
      StackTraceOptimizingLogger("MyLogger").print("", Exception("Hello"))
      expect(out.toString("UTF-8")).toContain("java.lang.Exception: Hello", "  at klite.").notToContain("  at ${javaClass.name}.cuts")
    }
    runBlocking { decorator.wrap(handler).invoke(mockk()) }
    KliteLogger.out = System.out
  }
}

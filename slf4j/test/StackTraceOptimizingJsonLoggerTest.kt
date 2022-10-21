package klite.slf4j

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEndWith
import ch.tutteli.atrium.api.fluent.en_GB.toStartWith
import ch.tutteli.atrium.api.verbs.expect
import com.fasterxml.jackson.databind.JsonNode
import klite.json.kliteJsonMapper
import klite.json.parse
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class StackTraceOptimizingJsonLoggerTest {
  @Test fun `puts stack trace into json in prod for PaperTrail`() {
    val out = ByteArrayOutputStream()
    KliteLogger.out = PrintStream(out)
    StackTraceOptimizingJsonLogger("MyLogger").print("", Exception("Hello\n\"World\"", IllegalStateException("cause")))
    val json = out.toString("UTF-8")
    expect(json).toStartWith("""{"error": "java.lang.Exception: Hello\n\"World\"", "stack": ["${javaClass.name}""")
      .toContain(""", "cause": {"error": "java.lang.IllegalStateException: cause", "stack": ["""")
      .toEndWith("]}}\n")
    kliteJsonMapper().parse<JsonNode>(json)
    KliteLogger.out = System.out
  }
}

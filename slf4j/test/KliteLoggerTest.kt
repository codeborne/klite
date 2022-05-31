package klite.slf4j

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.slf4j.KliteLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.Thread.currentThread

class KliteLoggerTest {
  val logger: Logger = KliteLogger("MyLogger")
  val out = ByteArrayOutputStream()

  @BeforeEach fun setUp() {
    KliteLogger.out = PrintStream(out)
  }

  @AfterEach fun tearDown() {
    KliteLogger.out = System.out
  }

  @Test fun isLevelEnabled() {
    expect(logger.isTraceEnabled).toEqual(false)
    expect(logger.isDebugEnabled).toEqual(false)
    expect(logger.isInfoEnabled).toEqual(true)
    expect(logger.isWarnEnabled).toEqual(true)
    expect(logger.isErrorEnabled).toEqual(true)
  }

  @Test fun logs() {
    logger.info("Hello")
    expect(out.toString("UTF-8")).toContain(" [${currentThread().name}] INFO MyLogger - Hello")
  }

  @Test fun `does not log`() {
    logger.debug("Hello")
    expect(out.toString("UTF-8")).toBeEmpty()
  }

  @Test fun `logs stack trace`() {
    logger.info("Failed", Exception("Kaboom"))
    expect(out.toString("UTF-8")).toContain("MyLogger - Failed", "java.lang.Exception: Kaboom", "\tat ")
  }

  @Test fun formats() {
    logger.info("Hello {}", "World")
    expect(out.toString("UTF-8")).toContain("- Hello World")
  }
}

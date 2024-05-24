package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.util.Date

class LoggerTest {
  @Test fun name() {
    expect(nonAnonymousClassName()).toEqual(LoggerTest::class.java.name)
    expect(object: Date() {}.nonAnonymousClassName()).toEqual(Date::class.java.name)
  }

  @Test fun testLogger() {
    expect(logger()).toBeAnInstanceOf<System.Logger>()
  }
}

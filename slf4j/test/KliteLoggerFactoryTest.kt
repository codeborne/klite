package klite.slf4j

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class KliteLoggerFactoryTest {
  val factory = KliteLoggerFactory()

  @Test fun `default class`() {
    expect(factory.getLogger("Hello")).toBeAnInstanceOf<KliteLogger>()
  }

  @Test fun `custom class`() {
    expect(factory.findConstructor(CustomLogger::class.qualifiedName!!).newInstance("")).toBeAnInstanceOf<CustomLogger>()
  }
}

class CustomLogger(name: String): KliteLogger(name)

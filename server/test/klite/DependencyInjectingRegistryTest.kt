package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toHaveSize
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class DependencyInjectingRegistryTest {
  val registry = DependencyInjectingRegistry()

  @Test fun require() {
    expect(registry.require<TextBodyParser>()).toBeAnInstanceOf<TextBodyParser>()
  }

  @Test fun requireAll() {
    expect(registry.requireAll<TextBodyParser>()).toBeEmpty()

    expect(registry.require<TextBodyParser>()).toBeAnInstanceOf<TextBodyParser>()
    registry.register<FormUrlEncodedParser>()

    expect(registry.requireAll<BodyParser>()).toHaveSize(2)
  }
}

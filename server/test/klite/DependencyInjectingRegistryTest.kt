package klite

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty

class DependencyInjectingRegistryTest {
  val registry = DependencyInjectingRegistry()

  @Test fun require() {
    expectThat(registry.require<TextBodyParser>()).isA<TextBodyParser>()
  }

  @Test fun requireAll() {
    expectThat(registry.requireAll<TextBodyParser>()).isEmpty()

    expectThat(registry.require<TextBodyParser>()).isA<TextBodyParser>()
    registry.register<FormUrlEncodedParser>()

    expectThat(registry.requireAll<BodyParser>()).hasSize(2)
  }
}

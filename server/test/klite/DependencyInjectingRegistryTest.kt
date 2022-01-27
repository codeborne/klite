package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DependencyInjectingRegistryTest {
  val registry = DependencyInjectingRegistry()

  @Test fun require() {
    assertThat(registry.require<TextBodyParser>()).isInstanceOf(TextBodyParser::class.java)
  }

  @Test fun requireAll() {
    assertThat(registry.requireAll<TextBodyParser>()).isEmpty()

    assertThat(registry.require<TextBodyParser>()).isInstanceOf(TextBodyParser::class.java)
    registry.register<FormUrlEncodedParser>()

    assertThat(registry.requireAll<BodyParser>()).hasSize(2)
  }
}

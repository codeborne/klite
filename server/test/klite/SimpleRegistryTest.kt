package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleRegistryTest {
  val registry = SimpleRegistry().apply {
    register<TextBodyParser>()
    register(FormUrlEncodedParser())
  }

  @Test
  fun require() {
    assertThat(registry.require<Registry>()).isSameAs(registry)
    assertThat(registry.require<TextBodyParser>()).isInstanceOf(TextBodyParser::class.java)
  }

  @Test
  fun requireAll() {
    assertThat(registry.requireAll<TextBodyParser>()).hasSize(1)
    assertThat(registry.requireAll<BodyParser>()).hasSize(2)
  }
}

package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleRegistryTest {
  val registry = SimpleRegistry().apply {
    register(TextBodyParser())
    register<FormUrlEncodedParser>()
    register(BodyRenderer::class, TextBodyRenderer::class)
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

  @Test
  fun `require class implementation`() {
    assertThat(registry.require<BodyRenderer>()).isInstanceOf(TextBodyRenderer::class.java)
  }
}

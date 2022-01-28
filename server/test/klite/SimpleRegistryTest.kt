package klite

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isSameInstanceAs

class SimpleRegistryTest {
  val registry = SimpleRegistry().apply {
    register(TextBodyParser())
    register<FormUrlEncodedParser>()
    register<BodyRenderer>(TextBodyRenderer::class)
  }

  @Test fun require() {
    expectThat(registry.require<Registry>()).isSameInstanceAs(registry)
    expectThat(registry.require<TextBodyParser>()).isA<TextBodyParser>()
  }

  @Test fun requireAll() {
    expectThat(registry.requireAll<TextBodyParser>()).hasSize(1)
    expectThat(registry.requireAll<BodyParser>()).hasSize(2)
  }

  @Test fun `require class implementation`() {
    expectThat(registry.require<BodyRenderer>()).isA<TextBodyRenderer>()
  }
}

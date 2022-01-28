package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toBeTheInstance
import ch.tutteli.atrium.api.fluent.en_GB.toHaveSize
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class SimpleRegistryTest {
  val registry = SimpleRegistry().apply {
    register(TextBodyParser())
    register<FormUrlEncodedParser>()
    register<BodyRenderer>(TextBodyRenderer::class)
  }

  @Test fun require() {
    expect(registry.require<Registry>()).toBeTheInstance(registry)
    expect(registry.require<TextBodyParser>()).toBeAnInstanceOf<TextBodyParser>()
  }

  @Test fun requireAll() {
    expect(registry.requireAll<TextBodyParser>()).toHaveSize(1)
    expect(registry.requireAll<BodyParser>()).toHaveSize(2)
  }

  @Test fun `require class implementation`() {
    expect(registry.require<BodyRenderer>()).toBeAnInstanceOf<TextBodyRenderer>()
  }
}

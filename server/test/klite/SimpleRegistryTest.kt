package klite

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beInstanceOf
import net.oddpoet.expect.extension.beSameInstance
import net.oddpoet.expect.extension.haveSizeOf
import org.junit.jupiter.api.Test

class SimpleRegistryTest {
  val registry = SimpleRegistry().apply {
    register(TextBodyParser())
    register<FormUrlEncodedParser>()
    register<BodyRenderer>(TextBodyRenderer::class)
  }

  @Test fun require() {
    expect(registry.require<Registry>()).to.beSameInstance(registry)
    expect(registry.require<TextBodyParser>()).to.beInstanceOf(TextBodyParser::class)
  }

  @Test fun requireAll() {
    expect(registry.requireAll<TextBodyParser>()).to.haveSizeOf(1)
    expect(registry.requireAll<BodyParser>()).to.haveSizeOf(2)
  }

  @Test fun `require class implementation`() {
    expect(registry.require<BodyRenderer>()).to.beInstanceOf(TextBodyRenderer::class)
  }
}

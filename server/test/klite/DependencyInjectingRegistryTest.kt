package klite

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beEmpty
import net.oddpoet.expect.extension.beInstanceOf
import net.oddpoet.expect.extension.haveSizeOf
import org.junit.jupiter.api.Test

class DependencyInjectingRegistryTest {
  val registry = DependencyInjectingRegistry()

  @Test fun require() {
    expect(registry.require<TextBodyParser>()).to.beInstanceOf(TextBodyParser::class)
  }

  @Test fun requireAll() {
    expect(registry.requireAll<TextBodyParser>()).to.beEmpty()

    expect(registry.require<TextBodyParser>()).to.beInstanceOf(TextBodyParser::class)
    registry.register<FormUrlEncodedParser>()

    expect(registry.requireAll<BodyParser>()).to.haveSizeOf(2)
  }
}

package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class RouterConfigTest {
  val config = object: RouterConfig(emptyList(), emptyList(), emptyList()) {
    override val registry = DependencyInjectingRegistry()
    override val pathParamRegexer = PathParamRegexer()
  }

  @Test fun `useOnly retains`() {
    config.renderers += TextBody()
    config.parsers += TextBody()
    config.parsers += FormUrlEncodedParser()
    config.useOnly<TextBody>()
    expect(config.renderers).toContainExactly { toBeAnInstanceOf<TextBody>() }
    expect(config.parsers).toContainExactly { toBeAnInstanceOf<TextBody>() }
  }

  @Test fun `useOnly adds if none`() {
    config.useOnly<TextBody>()
    expect(config.renderers).toContainExactly { toBeAnInstanceOf<TextBody>() }
    expect(config.parsers).toContainExactly { toBeAnInstanceOf<TextBody>() }
  }
}

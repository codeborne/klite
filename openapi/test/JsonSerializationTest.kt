package klite.openapi

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.swagger.v3.oas.annotations.enums.ParameterIn.PATH
import klite.jackson.kliteJsonMapper
import klite.jackson.render
import org.junit.jupiter.api.Test

class JsonSerializationTest {
  @Test fun `ParameterIn serialization using klite-json`() {
    expect(klite.json.JsonMapper().render(PATH)).toEqual("\"path\"")
  }

  @Test fun `ParameterIn serialization using jackson`() {
    expect(kliteJsonMapper().render(PATH)).toEqual("\"path\"")
  }
}

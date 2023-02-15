import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.json.JsonMapper
import org.junit.jupiter.api.Test

class JsonRendererTest {
  val mapper = JsonMapper()

  @Test fun literals() {
    expect(mapper.render(null)).toEqual("null")
    expect(mapper.render(true)).toEqual("true")
    expect(mapper.render(false)).toEqual("false")
    expect(mapper.render(123.45)).toEqual("123.45")
  }
}

package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.json.JsonOptions.Companion.TO_SNAKE_CASE
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JsonRendererTest {
  val mapper = JsonMapper()

  @Test fun literals() {
    expect(mapper.render(null)).toEqual("null")
    expect(mapper.render(true)).toEqual("true")
    expect(mapper.render(false)).toEqual("false")
    expect(mapper.render(123.45)).toEqual("123.45")
  }

  @Test fun string() {
    expect(mapper.render("Hello")).toEqual("\"Hello\"")
    expect(mapper.render("Hello\n\"World\"")).toEqual("\"Hello\\n\\\"World\\\"\"")
  }

  @Test fun array() {
    expect(mapper.render(emptyList<Any>())).toEqual("[]")
    expect(mapper.render(listOf("a", 1, 3))).toEqual("[\"a\",1,3]")
    expect(mapper.render(arrayOf(1, null))).toEqual("[1,null]")
  }

  @Test fun objects() {
    expect(mapper.render(emptyMap<Any, Any>())).toEqual("{}")
    expect(mapper.render(mapOf("x" to 123, "y" to "abc"))).toEqual("""{"x":123,"y":"abc"}""")
    expect(mapper.render(mapOf(1 to mapOf(2 to arrayOf(1, 2, 3))))).toEqual("""{"1":{"2":[1,2,3]}}""")
  }

  @Test fun snakeCase() {
    val mapper = JsonMapper(JsonOptions(keyConverter = TO_SNAKE_CASE))
    expect(mapper.render(mapOf("snakeCase" to 123))).toEqual("""{"snake_case":123}""")
  }

  @Test fun valueConverter() {
    val mapper = JsonMapper(JsonOptions(valueConverter = {
      if (it is LocalDate) it.year
      else it
    }))
    expect(mapper.render(mapOf("date" to LocalDate.of(2022, 10, 21)))).toEqual("""{"date":2022}""")
  }
}

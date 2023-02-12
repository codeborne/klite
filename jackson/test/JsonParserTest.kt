import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.json.JsonParser
import org.junit.jupiter.api.Test

class JsonParserTest {
  val parser = JsonParser()

  @Test fun parse() {
    expect(parser.parse("""  {  "hello" : "world", "blah": 123, "xxx": true, "zzz" : null, "nested":{"a":[],"c":{}}, "array": [1,2,3.14]}""")).toEqual(
      mapOf("hello" to "world", "blah" to 123, "xxx" to true, "zzz" to null, "nested" to mapOf("a" to emptyList<Any>(), "c" to emptyMap<String, Any>()), "array" to listOf(1, 2, 3.14)))
  }

  data class Hello(val hello: String)
}

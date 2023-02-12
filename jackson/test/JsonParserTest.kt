import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.json.JsonParser
import org.junit.jupiter.api.Test

class JsonParserTest {
  val parser = JsonParser()

  @Test fun parse() {
    expect(parser.parse("""  {  "hello" : "world", "blah": 123, "xxx": true, "zzz" : null, "nested":{"a":"b","c":{}}}""")).toEqual(
      mapOf("hello" to "world", "blah" to 123L, "xxx" to true, "zzz" to  null, "nested" to mapOf("a" to "b", "c" to emptyMap<String, Any>())))
  }

  data class Hello(val hello: String)
}

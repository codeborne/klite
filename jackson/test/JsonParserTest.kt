import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import klite.json.JsonParseException
import klite.json.JsonParser
import org.junit.jupiter.api.Test
import java.util.*

class JsonParserTest {
  val parser = JsonParser()

  @Test fun parse() {
    expect(parser.parse("""  {  "hello" : "world", "blah": 123, "xxx": true, "zzz" : null, "nested":{"a":[],"c":{}}, "array": [+1,-2,3.14, 1e20]}""")).toEqual(
      mapOf("hello" to "world", "blah" to 123, "xxx" to true, "zzz" to null, "nested" to mapOf("a" to emptyList<Any>(), "c" to emptyMap<String, Any>()), "array" to listOf(1, -2, 3.14, 1e20)))
  }

  @Test fun escaping() {
    expect(parser.parse("""{"x\\y": "\"\n\r\u00A0"}""")).toEqual(mapOf("x\\y" to "\"\n\r\u00A0"))
  }

  @Test fun `parse invalid`() {
    expect { parser.parse("""z""") }.toThrow<JsonParseException>().messageToContain("Unexpected char: z at index 0")
    expect { parser.parse("""{"hello": x""") }.toThrow<JsonParseException>().messageToContain("Unexpected char: x at index 10")
    expect { parser.parse("""{"hello": """") }.toThrow<JsonParseException>().messageToContain("Unfinished string, EOF at index 11")
    expect { parser.parse("""{"hello": 123""") }.toThrow<JsonParseException>().messageToContain("Expecting , but got EOF at index 13")
    expect { parser.parse("""nulls""") }.toThrow<JsonParseException>().messageToContain("Unexpected nulls at index 5")
    expect { parser.parse("""123.12.12""") }.toThrow<NumberFormatException>().messageToContain("multiple points")
  }

  @Test fun `parse into class`() {
    expect(parser.parse("""{"hello": "world", "id": "b8ca58ec-ab15-11ed-93cc-8fdb43988a14"}""", Hello::class))
      .toEqual(Hello("world", UUID.fromString("b8ca58ec-ab15-11ed-93cc-8fdb43988a14")))
  }

  data class Hello(val hello: String, val id: UUID)
}

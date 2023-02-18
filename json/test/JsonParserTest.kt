package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.reflect.typeOf

class JsonParserTest {
  val mapper = JsonMapper()

  @Test fun parse() {
    expect(mapper.parse<Map<String, Any?>>("""  {  "hello" : "world", "blah": 123, "xxx": true, "zzz" : null, "nested":{"a":[],"c":{}}, "array": [+1,-2,3.14, 1e20]}""")).toEqual(
      mapOf("hello" to "world", "blah" to 123, "xxx" to true, "zzz" to null, "nested" to mapOf("a" to emptyList<Any>(), "c" to emptyMap<String, Any>()), "array" to listOf(1, -2, 3.14, 1e20)))
  }

  @Test fun escaping() {
    expect(mapper.parse<Map<String, Any?>>("""{"x\\y": "\"\n\r\u00A0"}""")).toEqual(mapOf("x\\y" to "\"\n\r\u00A0"))
  }

  @Test fun `parse invalid`() {
    expect { mapper.parse<Any>("""z""") }.toThrow<JsonParseException>().messageToContain("Unexpected char: z at index 0")
    expect { mapper.parse<Any>("""{"hello": x""") }.toThrow<JsonParseException>().messageToContain("Unexpected char: x at index 10")
    expect { mapper.parse<Any>("""{"hello": """") }.toThrow<JsonParseException>().messageToContain("Unfinished string, EOF at index 11")
    expect { mapper.parse<Any>("""{"hello": 123""") }.toThrow<JsonParseException>().messageToContain("Expecting , but got EOF at index 13")
    expect { mapper.parse<Any>("""nulls""") }.toThrow<JsonParseException>().messageToContain("Unexpected nulls at index 5")
    expect { mapper.parse<Any>("""123.12.12""") }.toThrow<NumberFormatException>().messageToContain("multiple points")
  }

  @Test fun `parse into class`() {
    expect(mapper.parse<Hello>("""{
      "hello": "", "id": "b8ca58ec-ab15-11ed-93cc-8fdb43988a14", "date": "2022-10-21", "instant": "2022-10-21T10:55:00Z",
      "nested": {"x": 567}, "array": [{}, {}]}
    """, typeOf<Hello>())).toEqual(
      Hello("", UUID.fromString("b8ca58ec-ab15-11ed-93cc-8fdb43988a14"), LocalDate.parse("2022-10-21"), Instant.parse("2022-10-21T10:55:00Z"), Nested(567.toBigDecimal()),
        listOf(Nested(), Nested())))
  }

  data class Hello(val hello: String, val id: UUID, val date: LocalDate, val instant: Instant, val nested: Nested, val array: List<Nested> = emptyList())
  data class Nested(val x: BigDecimal = ZERO, val y: Int = 123)

  @Test fun trimToNull() {
    val parser = JsonMapper(JsonOptions(trimToNull = true))
    expect(parser.parse<Nullable>("""{"x": "", "unknown": 123}""", typeOf<Nullable>())).toEqual(Nullable())
  }

  @Test fun `snake case`() {
    val mapper = JsonMapper(JsonOptions(keys = SnakeCase()))
    expect(mapper.parse<Any>("""{"hello_world_is_good": 0}""")).toEqual(mapOf("helloWorldIsGood" to 0))
  }

  data class Nullable(val x: String? = null)
}

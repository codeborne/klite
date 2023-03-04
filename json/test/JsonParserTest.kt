package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import klite.TSID
import klite.uuid
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.Instant
import java.time.LocalDate
import java.util.*

class JsonParserTest {
  val mapper = JsonMapper()

  @Test fun parse() {
    expect(mapper.parse<Map<String, Any?>>("""  {  "hello" : "world", "blah": 123, "xxx": true, "zzz" : null, "nested":{"a":[],"c":{}}, "array": [+1,-2,3.14, 1e20]}""")).toEqual(
      mapOf("hello" to "world", "blah" to 123, "xxx" to true, "zzz" to null, "nested" to mapOf("a" to emptyList<Any>(), "c" to emptyMap<String, Any>()), "array" to listOf(1, -2, 3.14, 1e20)))
  }

  @Test fun array() {
    expect(mapper.parse<List<Int>>(" [1,\n2,3]\n")).toEqual(listOf(1, 2, 3))
    expect(mapper.parse<Set<Int>>(" [1,\n2,3]\n")).toEqual(setOf(1, 2, 3))
  }

  @Test fun escaping() {
    expect(mapper.copy(trimToNull = false).parse<Any>("""{"x\\y": "\"\n\r\u00A0"}""")).toEqual(mapOf("x\\y" to "\"\n\r\u00A0"))
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
      "hellou": " x ", "id": "b8ca58ec-ab15-11ed-93cc-8fdb43988a14", "date": "2022-10-21", "instant": "2022-10-21T10:55:00Z",
      "nested": {"x": 567}, "array": [{}, {"x": 2}], "map": {"2022-10-21": {"y": 1}},"ignore": false, "readOnly":  false}
    """)).toEqual(
      Hello("x", "b8ca58ec-ab15-11ed-93cc-8fdb43988a14".uuid, LocalDate.parse("2022-10-21"), Instant.parse("2022-10-21T10:55:00Z"), Nested(567.toBigDecimal()),
        listOf(Nested(), Nested(x = 2.toBigDecimal())), mapOf(LocalDate.parse("2022-10-21") to Nested(y = 1))))
  }

  @Test fun trimToNull() {
    val json = """{"x": "", "unknown": 123}"""
    expect(mapper.parse<Nullable>(json)).toEqual(Nullable())

    val mapper = mapper.copy(trimToNull = false)
    expect(mapper.parse<Nullable>(json)).toEqual(Nullable(""))
  }

  @Test fun converter() {
    val uuid = UUID.randomUUID()
    expect(mapper.parse<UUID>("\"$uuid\"")).toEqual(uuid)
    val tsid = TSID()
    expect(mapper.parse<TSID>("\"$tsid\"")).toEqual(tsid)

    expect(mapper.parse<LocalDate>("\"2022-12-23\"")).toEqual(LocalDate.of(2022, 12, 23))
  }

  @Test fun `snake case`() {
    val mapper = JsonMapper(keys = SnakeCase)
    expect(mapper.parse<Any>("""{"hello_world_is_good": 0}""")).toEqual(mapOf("helloWorldIsGood" to 0))
  }

  @Test fun `upper camel case`() {
    val mapper = JsonMapper(keys = Capitalize)
    expect(mapper.parse<Any>("""{"HelloWorld": true}""")).toEqual(mapOf("helloWorld" to true))
  }

  data class Nullable(val x: String? = null)
}

data class Hello(@JsonProperty("hellou") val hello: String, val id: UUID, val date: LocalDate, val instant: Instant, val nested: Nested,
                 val array: List<Nested> = emptyList(), val map: Map<LocalDate, Nested> = emptyMap(), val nullable: String? = null,
                 @JsonIgnore val ignore: Boolean = true, @JsonProperty(readOnly = true) val readOnly: Boolean = true)
data class Nested(val x: BigDecimal = ZERO, val y: Int = 123)

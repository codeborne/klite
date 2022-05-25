package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import klite.ErrorResponse
import klite.StatusCode.Companion.BadRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class JsonBodyTest {
  val jsonBody = JsonBody()

  @Test fun `can create Kotlin classes`() {
    val someData = jsonBody.parse("""{"hello":"World", "nullableValue": "Value"}""".byteInputStream(), SomeData::class)
    expect(someData).toEqual(SomeData("World", nullableValue = Value("Value")))
  }

  @Test fun `coerces empty strings as nulls`() {
    val someData = jsonBody.parse("""{"hello":"World", "nullable": "", "nullableValue": ""}""".byteInputStream(), SomeData::class)
    expect(someData).toEqual(SomeData("World"))
  }

  @Test fun `more meaningful error messages from MissingKotlinParameterException`() {
    try {
      jsonBody.parse("{}".byteInputStream(), SomeData::class)
      fail("Expecting MissingKotlinParameterException")
    } catch (e: MissingKotlinParameterException) {
      expect(jsonBody.handleMissingParameter(e)).toEqual(ErrorResponse(BadRequest, "hello is required"))
    }
  }

  @Test fun `more meaningful error messages from ValueInstantiationException`() {
    try {
      jsonBody.parse("""{"hello":"Illegal stuff"}""".byteInputStream(), SomeData::class)
      fail("Expecting ValueInstantiationException")
    } catch (e: ValueInstantiationException) {
      expect(jsonBody.handleValueInstantiation(e)).toEqual(ErrorResponse(BadRequest, "Illegal stuff in hello"))
    }
  }
}

data class SomeData(val hello: String, val nullable: String? = null, val nullableValue: Value? = null) {
  init {
    require(!hello.contains("Illegal")) { "Illegal stuff in hello" }
  }
}

// TODO: register values classes with klite Converter if they have @JsonCreator and @JsonValue annotations
// then they will also be supported by JdbcConverter
data class Value @JsonCreator(mode = DELEGATING) constructor(@JsonValue val value: String)

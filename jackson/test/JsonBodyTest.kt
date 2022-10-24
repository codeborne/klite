package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import klite.Converter
import klite.ErrorResponse
import klite.StatusCode.Companion.BadRequest
import klite.trimToNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import kotlin.reflect.typeOf

class JsonBodyTest {
  init { Converter.use { ConverterValue(it) } }
  val jsonBody = JsonBody()

  @Test fun `can create Kotlin classes`() {
    val someData: SomeData = jsonBody.parse("""{"hello":"World", "nullableValue": "Value"}""".byteInputStream(), typeOf<SomeData>())
    expect(someData).toEqual(SomeData("World", nullableValue = Value("Value")))
  }

  @Test fun `coerces empty strings as nulls`() {
    val someData: SomeData = jsonBody.parse("""{"hello":"World", "nullable": "", "nullableValue": ""}""".byteInputStream(), typeOf<SomeData>())
    expect(someData).toEqual(SomeData("World"))
  }

  @Test fun `uses converter to convert values`() {
    val value = jsonBody.parse<ConverterValue>("\"value\"".byteInputStream(), typeOf<ConverterValue>())
    expect(value).toEqual(ConverterValue("value"))
  }

  @Test fun `more meaningful error messages from MissingKotlinParameterException`() {
    try {
      jsonBody.parse<SomeData>("{}".byteInputStream(), typeOf<SomeData>())
      fail("Expecting MissingKotlinParameterException")
    } catch (e: MissingKotlinParameterException) {
      expect(jsonBody.handleMissingParameter(e)).toEqual(ErrorResponse(BadRequest, "hello is required"))
    }
  }

  @Test fun `more meaningful error messages from ValueInstantiationException`() {
    try {
      jsonBody.parse<SomeData>("""{"hello":"Illegal stuff"}""".byteInputStream(), typeOf<SomeData>())
      fail("Expecting ValueInstantiationException")
    } catch (e: ValueInstantiationException) {
      expect(jsonBody.handleValueInstantiation(e)).toEqual(ErrorResponse(BadRequest, "Illegal stuff in hello"))
    }
  }

  @Test fun `parameterized types`() {
    val set = jsonBody.parse<Set<BigDecimal>>("""[1,2]""".byteInputStream(), typeOf<Set<BigDecimal>>())
    expect(set).toEqual(setOf(1.toBigDecimal(), 2.toBigDecimal()))
  }
}

data class SomeData(val hello: String, val nullable: String? = null, val nullableValue: Value? = null) {
  init {
    require(!hello.contains("Illegal")) { "Illegal stuff in hello" }
  }
}

data class Value(@JsonValue val value: String) {
  companion object {
    @JsonCreator(mode = DELEGATING) @JvmStatic fun create(value: String?) = value?.trimToNull()?.let { Value(it) }
  }
}

data class ConverterValue(val value: String)

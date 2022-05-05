package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import klite.ErrorResponse
import klite.StatusCode.Companion.BadRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class JsonBodyTest {
  val jsonBody = JsonBody()

  @Test fun `can create Kotlin classes`() {
    val someData = jsonBody.parse("""{"hello":"World"}""".byteInputStream(), SomeData::class)
    expect(someData).toEqual(SomeData("World"))
  }

  @Test fun `parses empty strings to null`() {
    val someData = jsonBody.parse("""{"hello":"World", "nullable": ""}""".byteInputStream(), SomeData::class)
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

data class SomeData(val hello: String, val nullable: String? = null) {
  init {
    require(!hello.contains("Illegal")) { "Illegal stuff in hello" }
  }
}


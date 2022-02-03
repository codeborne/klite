package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
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

  @Test fun `more meaningful error messages`() {
    try {
      jsonBody.parse("{}".byteInputStream(), SomeData::class)
      fail("Expecting MissingKotlinParameterException")
    } catch (e: MissingKotlinParameterException) {
      expect(jsonBody.handleMissingParameter(e)).toEqual(ErrorResponse(BadRequest, "hello is required"))
    }
  }
}

data class SomeData(val hello: String)


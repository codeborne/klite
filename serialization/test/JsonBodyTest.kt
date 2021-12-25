package klite.serialization

import klite.ErrorResponse
import klite.StatusCode
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class JsonBodyTest {
  val jsonBody = JsonBody()

  @Test fun `can create data classes`() {
    val someData = jsonBody.parse("""{"email":"a@b.ee","date":"2021-12-12","extra":123}""".byteInputStream(), SomeData::class)
    assertThat(someData).isEqualTo(SomeData(Email("a@b.ee"), LocalDate.parse("2021-12-12")))
  }

  @Test fun `can serialize ErrorResponse`() {
    val out = ByteArrayOutputStream()
    jsonBody.render(out, ErrorResponse(StatusCode.NotFound, "/"))
    assertThat(out.toByteArray().decodeToString()).isEqualTo("{\"statusCode\":404,\"reason\":\"Not Found\",\"message\":\"/\"}")
  }
}

@Serializable @JvmInline value class Email(val email: String)
@Serializable data class SomeData(val email: Email, val date: LocalDate, val optional: String? = null)

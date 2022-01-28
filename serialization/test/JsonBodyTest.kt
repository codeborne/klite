@file:UseSerializers(UUIDSerializer::class)
package klite.serialization

import klite.ErrorResponse
import klite.StatusCode
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.*

class JsonBodyTest {
  val jsonBody = JsonBody()

  @Test fun `can create data classes`() {
    val someData = jsonBody.parse("""{"email":"a@b.ee","date":"2021-12-12","extra":123}""".byteInputStream(), SomeData::class)
    expect(someData).to.equal(SomeData(Email("a@b.ee"), LocalDate.parse("2021-12-12")))
  }

  @Test fun `can serialize ErrorResponse`() {
    val out = ByteArrayOutputStream()
    jsonBody.render(out, ErrorResponse(StatusCode.NotFound, "/"))
    expect(out.toByteArray().decodeToString()).to.equal("""{"statusCode":404,"reason":"Not Found","message":"/"}""")
  }

  @Test fun `use ConverterSerializer`() {
    val out = ByteArrayOutputStream()
    val entity = SomeEntity(UUID.fromString("fc587008-f555-4b4d-82c0-818b05eb8bad"))
    jsonBody.render(out, entity)
    expect(out.toByteArray().decodeToString()).to.equal("""{"id":"fc587008-f555-4b4d-82c0-818b05eb8bad"}""")

    expect(jsonBody.parse("""{"id":"fc587008-f555-4b4d-82c0-818b05eb8bad"}""".byteInputStream(), SomeEntity::class)).to.equal(entity)
  }
}

@Serializable @JvmInline value class Email(val email: String)
@Serializable data class SomeData(val email: Email, val date: LocalDate, val optional: String? = null)

@Serializable data class SomeEntity(val id: UUID)

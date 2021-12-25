package klite.serialization

import klite.ErrorResponse
import klite.StatusCode
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.*

class JsonBodyTest {
  val jsonBody = JsonBody()

  @Test fun `can create data classes`() {
    val someData = jsonBody.parse("""{"email":"a@b.ee","date":"2021-12-12","extra":123}""".byteInputStream(), SomeData::class)
    assertThat(someData).isEqualTo(SomeData(Email("a@b.ee"), LocalDate.parse("2021-12-12")))
  }

  @Test fun `can serialize ErrorResponse`() {
    val out = ByteArrayOutputStream()
    jsonBody.render(out, ErrorResponse(StatusCode.NotFound, "/"))
    assertThat(out.toByteArray().decodeToString()).isEqualTo("""{"statusCode":404,"reason":"Not Found","message":"/"}""")
  }

  @Test fun `can serialize using toString`() {
    val out = ByteArrayOutputStream()
    jsonBody.render(out, SomeEntity(UUID.fromString("fc587008-f555-4b4d-82c0-818b05eb8bad")))
    assertThat(out.toByteArray().decodeToString()).isEqualTo("""{"id":"fc587008-f555-4b4d-82c0-818b05eb8bad"}""")
  }
}

@Serializable @JvmInline value class Email(val email: String)
@Serializable data class SomeData(val email: Email, val date: LocalDate, val optional: String? = null)

@Serializable data class SomeEntity(@Serializable(with = UUIDSerializer::class) val id: UUID)

class UUIDSerializer: KSerializer<UUID> {
  override val descriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder) = UUID.fromString(decoder.decodeString())
}

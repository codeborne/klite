package klite.serialization

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonBodyTest {
  @Test fun `can create data classes`() {
    val someData = JsonBody().parse("""{"email":"a@b.ee","date":"2021-12-12","extra":123}""".byteInputStream(), SomeData::class)
    assertThat(someData).isEqualTo(SomeData(Email("a@b.ee"), LocalDate.parse("2021-12-12")))
  }
}

@Serializable @JvmInline value class Email(val email: String)
@Serializable data class SomeData(val email: Email, val date: LocalDate, val optional: String? = null)

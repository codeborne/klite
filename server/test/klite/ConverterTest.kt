package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.util.*

class ConverterTest {
  @Test
  fun `pre-defined`() {
    assertThat(Converter.fromString<UUID>("05e1cebe-67dc-4780-b5df-472edd55fab6")).isEqualTo(UUID.fromString("05e1cebe-67dc-4780-b5df-472edd55fab6"))
  }

  @Test
  fun enum() {
    assertThat(Converter.fromString<AnnotationTarget>("FIELD")).isEqualTo(AnnotationTarget.FIELD)
  }

  @Test
  fun constructor() {
    assertThat(Converter.fromString<URI>("http://hello/")).isEqualTo(URI("http://hello/"))
    assertThat(Converter.fromString<Int>("123")).isEqualTo(123)
    assertThat(Converter.fromString<Long>("123")).isEqualTo(123L)
  }

  @Test
  fun javaPrimitive() {
    assertThat(Converter.fromString("456", Int::class)).isEqualTo(456)
  }

  @Test
  fun parse() {
    assertThat(Converter.fromString<LocalDate>("2021-10-21")).isEqualTo(LocalDate.parse("2021-10-21"))
    assertThat(Converter.fromString<Period>("P1D")).isEqualTo(Period.parse("P1D"))
  }

  @Test
  fun `no creator`() {
    assertThrows<IllegalStateException> {
      Converter.fromString<Converter>("some string")
    }
  }
}

package server.annotations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.util.*

class TypeConverterTest {
  val converter = TypeConverter()

  @Test
  fun `pre-defined`() {
    assertThat(converter.fromString<UUID>("05e1cebe-67dc-4780-b5df-472edd55fab6")).isEqualTo(UUID.fromString("05e1cebe-67dc-4780-b5df-472edd55fab6"))
  }

  @Test
  fun constructor() {
    assertThat(converter.fromString<URI>("http://hello/")).isEqualTo(URI("http://hello/"))
  }

  @Test
  fun parse() {
    assertThat(converter.fromString<LocalDate>("2021-10-21")).isEqualTo(LocalDate.parse("2021-10-21"))
    assertThat(converter.fromString<Period>("P1D")).isEqualTo(Period.parse("P1D"))
  }

  @Test
  fun `no creator`() {
    assertThrows<IllegalStateException> {
      converter.fromString<TypeConverter>("some string")
    }
  }
}

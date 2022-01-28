package klite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.util.*

class ConverterTest {
  @Test fun `pre-defined`() {
    expectThat(Converter.fromString<UUID>("05e1cebe-67dc-4780-b5df-472edd55fab6")).isEqualTo(UUID.fromString("05e1cebe-67dc-4780-b5df-472edd55fab6"))
  }

  @Test fun enum() {
    expectThat(Converter.fromString<AnnotationTarget>("FIELD")).isEqualTo(AnnotationTarget.FIELD)
  }

  @Test fun constructor() {
    expectThat(Converter.fromString<URI>("http://hello/")).isEqualTo(URI("http://hello/"))
    expectThat(Converter.fromString<Int>("123")).isEqualTo(123)
    expectThat(Converter.fromString<Long>("123")).isEqualTo(123L)
  }

  @Test fun jvmInline() {
    expectThat(Converter.fromString<Inline>("hello")).isEqualTo(Inline("hello"))
  }

  @Test fun javaPrimitive() {
    expectThat(Converter.fromString("456", Int::class)).isEqualTo(456)
  }

  @Test fun parse() {
    expectThat(Converter.fromString<LocalDate>("2021-10-21")).isEqualTo(LocalDate.parse("2021-10-21"))
    expectThat(Converter.fromString<Period>("P1D")).isEqualTo(Period.parse("P1D"))
  }

  @Test fun `no creator`() {
    assertThrows<IllegalStateException> {
      Converter.fromString<Converter>("some string")
    }
  }
}

@JvmInline value class Inline(val string: String)

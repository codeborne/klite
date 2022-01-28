package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.util.*

class ConverterTest {
  @Test fun `pre-defined`() {
    expect(Converter.fromString<UUID>("05e1cebe-67dc-4780-b5df-472edd55fab6")).toEqual(UUID.fromString("05e1cebe-67dc-4780-b5df-472edd55fab6"))
  }

  @Test fun enum() {
    expect(Converter.fromString<AnnotationTarget>("FIELD")).toEqual(AnnotationTarget.FIELD)
  }

  @Test fun constructor() {
    expect(Converter.fromString<URI>("http://hello/")).toEqual(URI("http://hello/"))
    expect(Converter.fromString<Int>("123")).toEqual(123)
    expect(Converter.fromString<Long>("123")).toEqual(123L)
  }

  @Test fun jvmInline() {
    expect(Converter.fromString<Inline>("hello")).toEqual(Inline("hello"))
  }

  @Test fun javaPrimitive() {
    expect(Converter.fromString("456", Int::class)).toEqual(456)
  }

  @Test fun parse() {
    expect(Converter.fromString<LocalDate>("2021-10-21")).toEqual(LocalDate.parse("2021-10-21"))
    expect(Converter.fromString<Period>("P1D")).toEqual(Period.parse("P1D"))
  }

  @Test fun `no creator`() {
    assertThrows<IllegalStateException> {
      Converter.fromString<Converter>("some string")
    }
  }
}

@JvmInline value class Inline(val string: String)

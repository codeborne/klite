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
    expect(Converter.from<UUID>("05e1cebe-67dc-4780-b5df-472edd55fab6")).toEqual(UUID.fromString("05e1cebe-67dc-4780-b5df-472edd55fab6"))
  }

  @Test fun enum() {
    expect(Converter.from<AnnotationTarget>("FIELD")).toEqual(AnnotationTarget.FIELD)
  }

  @Test fun constructor() {
    expect(Converter.from<URI>("http://hello/")).toEqual(URI("http://hello/"))
    expect(Converter.from<Int>("123")).toEqual(123)
    expect(Converter.from<Long>("123")).toEqual(123L)
  }

  @Test fun jvmInline() {
    expect(Converter.from<Inline>("hello")).toEqual(Inline("hello"))
  }

  @Test fun javaPrimitive() {
    expect(Converter.from("456", Int::class)).toEqual(456)
  }

  @Test fun parse() {
    expect(Converter.from<LocalDate>("2021-10-21")).toEqual(LocalDate.parse("2021-10-21"))
    expect(Converter.from<Period>("P1D")).toEqual(Period.parse("P1D"))
  }

  @Test fun `no creator`() {
    assertThrows<IllegalStateException> {
      Converter.from<Converter>("some string")
    }
  }
}

@JvmInline value class Inline(val string: String)

package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.UUID.fromString
import java.util.UUID.randomUUID
import java.util.regex.Pattern

class ConverterTest {
  @Test fun `pre-defined`() {
    expect(Converter.from<UUID>("05e1cebe-67dc-4780-b5df-472edd55fab6")).toEqual(UUID.fromString("05e1cebe-67dc-4780-b5df-472edd55fab6"))
    expect(Converter.from<Currency>("EUR")).toEqual(Currency.getInstance("EUR"))
    expect(Converter.from<Locale>("et_EE")).toEqual(Locale("et", "EE"))
  }

  @Test fun custom() {
    expect(Converter.supports(Pattern::class)).toEqual(false)
    expect { Converter.from<Pattern>("[a-z]") }.toThrow<IllegalStateException>()

    Converter.use<Pattern> { Pattern.compile(it) }

    expect(Converter.from<Pattern>("[a-z]").pattern()).toEqual("[a-z]")
    expect(Converter.supports(Pattern::class)).toEqual(true)
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
    val id = randomUUID()
    Converter.use { InlineId(fromString(it)) }
    expect(Converter.from<InlineId>(id.toString())).toEqual(InlineId(id))
  }

  @Test fun javaPrimitive() {
    expect(Converter.from("456", Int::class)).toEqual(456)
  }

  @Test fun parse() {
    expect(Converter.supports(Locale::class)).toEqual(true)
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
@JvmInline value class InlineId(val id: UUID)

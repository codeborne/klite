package klite

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.util.*

class ConverterTest {
  @Test fun `pre-defined`() {
    expect(Converter.fromString<UUID>("05e1cebe-67dc-4780-b5df-472edd55fab6")).to.equal(UUID.fromString("05e1cebe-67dc-4780-b5df-472edd55fab6"))
  }

  @Test fun enum() {
    expect(Converter.fromString<AnnotationTarget>("FIELD")).to.equal(AnnotationTarget.FIELD)
  }

  @Test fun constructor() {
    expect(Converter.fromString<URI>("http://hello/")).to.equal(URI("http://hello/"))
    expect(Converter.fromString<Int>("123")).to.equal(123)
    expect(Converter.fromString<Long>("123")).to.equal(123L)
  }

  @Test fun jvmInline() {
    expect(Converter.fromString<Inline>("hello")).to.equal(Inline("hello"))
  }

  @Test fun javaPrimitive() {
    expect(Converter.fromString("456", Int::class)).to.equal(456)
  }

  @Test fun parse() {
    expect(Converter.fromString<LocalDate>("2021-10-21")).to.equal(LocalDate.parse("2021-10-21"))
    expect(Converter.fromString<Period>("P1D")).to.equal(Period.parse("P1D"))
  }

  @Test fun `no creator`() {
    assertThrows<IllegalStateException> {
      Converter.fromString<Converter>("some string")
    }
  }
}

@JvmInline value class Inline(val string: String)

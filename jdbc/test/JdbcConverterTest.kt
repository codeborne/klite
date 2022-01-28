import ch.tutteli.atrium.api.fluent.en_GB.toBeTheInstance
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.jdbc.JdbcConverter.toDBType
import org.junit.jupiter.api.Test
import java.time.*
import java.time.ZoneOffset.UTC
import java.util.*

class JdbcConverterTest {
  @Test fun `null`() {
    expect(toDBType(null)).toEqual(null)
  }

  @Test fun `primitive types are supported by jdbc`() {
    expect(toDBType(123)).toEqual(123)
    expect(toDBType(123L)).toEqual(123L)
    expect(toDBType(123.0)).toEqual(123.0)
  }

  @Test fun `BigInteger and BigDecimal`() {
    expect(toDBType(123.toBigDecimal())).toEqual(123.toBigDecimal())
    expect(toDBType(123.toBigInteger())).toEqual(123.toBigInteger())
  }

  @Test fun `local date and time`() {
    expect(toDBType(LocalDate.of(2021, 10, 21))).toEqual(LocalDate.of(2021, 10, 21))
    expect(toDBType(LocalDateTime.MIN)).toBeTheInstance(LocalDateTime.MIN)
    expect(toDBType(LocalTime.MIN)).toBeTheInstance(LocalTime.MIN)
    expect(toDBType(OffsetDateTime.MIN)).toBeTheInstance(OffsetDateTime.MIN)
  }

  @Test fun `Instant should be converted to offset`() {
    expect(toDBType(Instant.EPOCH)).toEqual(Instant.EPOCH.atOffset(UTC))
  }

  @Test fun `toString types`() {
    expect(toDBType(Currency.getInstance("EUR"))).toEqual("EUR")
    expect(toDBType(Locale("et"))).toEqual("et")
    expect(toDBType(Period.ofDays(3))).toEqual("P3D")
  }
}

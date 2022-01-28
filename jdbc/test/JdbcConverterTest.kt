import klite.jdbc.JdbcConverter.toDBType
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.time.*
import java.time.ZoneOffset.UTC
import java.util.*

class JdbcConverterTest {
  @Test fun `null`() {
    expectThat(toDBType(null)).isNull()
  }

  @Test fun `primitive types are supported by jdbc`() {
    expectThat(toDBType(123)).isEqualTo(123)
    expectThat(toDBType(123L)).isEqualTo(123L)
    expectThat(toDBType(123.0)).isEqualTo(123.0)
  }

  @Test fun `BigInteger and BigDecimal`() {
    expectThat(toDBType(123.toBigDecimal())).isEqualTo(123.toBigDecimal())
    expectThat(toDBType(123.toBigInteger())).isEqualTo(123.toBigInteger())
  }

  @Test fun `local date and time`() {
    expectThat(toDBType(LocalDate.of(2021, 10, 21))).isEqualTo(LocalDate.of(2021, 10, 21))
    expectThat(toDBType(LocalDateTime.MIN)).isSameInstanceAs(LocalDateTime.MIN)
    expectThat(toDBType(LocalTime.MIN)).isSameInstanceAs(LocalTime.MIN)
    expectThat(toDBType(OffsetDateTime.MIN)).isSameInstanceAs(OffsetDateTime.MIN)
  }

  @Test fun `Instant should be converted to offset`() {
    expectThat(toDBType(Instant.EPOCH)).isEqualTo(Instant.EPOCH.atOffset(UTC))
  }

  @Test fun `toString types`() {
    expectThat(toDBType(Currency.getInstance("EUR"))).isEqualTo("EUR")
    expectThat(toDBType(Locale("et"))).isEqualTo("et")
    expectThat(toDBType(Period.ofDays(3))).isEqualTo("P3D")
  }
}

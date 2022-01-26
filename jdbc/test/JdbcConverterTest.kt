import klite.jdbc.JdbcConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.*
import java.time.ZoneOffset.UTC
import java.util.*

class JdbcConverterTest {
  @Test fun `null`() {
    assertThat(JdbcConverter.toDBType(null)).isNull()
  }

  @Test fun `primitive types are supported by jdbc`() {
    assertThat(JdbcConverter.toDBType(123)).isEqualTo(123)
    assertThat(JdbcConverter.toDBType(123L)).isEqualTo(123L)
    assertThat(JdbcConverter.toDBType(123.0)).isEqualTo(123.0)
  }

  @Test fun `BigInteger and BigDecimal`() {
    assertThat(JdbcConverter.toDBType(123.toBigDecimal())).isEqualTo(123.toBigDecimal())
    assertThat(JdbcConverter.toDBType(123.toBigInteger())).isEqualTo(123.toBigInteger())
  }

  @Test fun `local date and time`() {
    assertThat(JdbcConverter.toDBType(LocalDate.of(2021, 10, 21))).isEqualTo(LocalDate.of(2021, 10, 21))
    assertThat(JdbcConverter.toDBType(LocalDateTime.MIN)).isEqualTo(LocalDateTime.MIN)
    assertThat(JdbcConverter.toDBType(LocalTime.MIN)).isEqualTo(LocalTime.MIN)
    assertThat(JdbcConverter.toDBType(OffsetDateTime.MIN)).isEqualTo(OffsetDateTime.MIN)
  }

  @Test fun `Instant should be converted to offset`() {
    assertThat(JdbcConverter.toDBType(Instant.EPOCH)).isEqualTo(Instant.EPOCH.atOffset(UTC))
  }

  @Test fun `toString types`() {
    assertThat(JdbcConverter.toDBType(Currency.getInstance("EUR"))).isEqualTo("EUR")
    assertThat(JdbcConverter.toDBType(Locale("et"))).isEqualTo("et")
    assertThat(JdbcConverter.toDBType(Period.ofDays(3))).isEqualTo("P3D")
  }
}

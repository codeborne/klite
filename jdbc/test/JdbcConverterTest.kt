import klite.jdbc.JdbcConverter.toDBType
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beNull
import net.oddpoet.expect.extension.beSameInstance
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.*
import java.time.ZoneOffset.UTC
import java.util.*

class JdbcConverterTest {
  @Test fun `null`() {
    expect(toDBType(null)).to.beNull()
  }

  @Test fun `primitive types are supported by jdbc`() {
    expect(toDBType(123)).to.equal(123)
    expect(toDBType(123L)).to.equal(123L)
    expect(toDBType(123.0)).to.equal(123.0)
  }

  @Test fun `BigInteger and BigDecimal`() {
    expect(toDBType(123.toBigDecimal())).to.equal(123.toBigDecimal())
    expect(toDBType(123.toBigInteger())).to.equal(123.toBigInteger())
  }

  @Test fun `local date and time`() {
    expect(toDBType(LocalDate.of(2021, 10, 21))).to.equal(LocalDate.of(2021, 10, 21))
    assertSame(LocalDateTime.MIN, toDBType(LocalDateTime.MIN))
    expect(toDBType(LocalTime.MIN)).to.beSameInstance(LocalTime.MIN)
    expect(toDBType(OffsetDateTime.MIN)).to.beSameInstance(OffsetDateTime.MIN)
  }

  @Test fun `Instant should be converted to offset`() {
    expect(toDBType(Instant.EPOCH)).to.equal(Instant.EPOCH.atOffset(UTC))
  }

  @Test fun `toString types`() {
    expect(toDBType(Currency.getInstance("EUR"))).to.equal("EUR")
    expect(toDBType(Locale("et"))).to.equal("et")
    expect(toDBType(Period.ofDays(3))).to.equal("P3D")
  }
}

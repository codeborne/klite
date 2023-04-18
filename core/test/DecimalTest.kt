import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThan
import ch.tutteli.atrium.api.fluent.en_GB.toBeLessThan
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import klite.Decimal
import klite.d
import org.junit.jupiter.api.Test
import java.text.NumberFormat
import java.util.*

class DecimalTest {
  @Test fun `operators & string`() {
    expect(20.d.toString()).toEqual("20.00")
    expect((3.d * 5.d).toString()).toEqual("15.00")
    expect((3.d / 5.d).toString()).toEqual("0.60")
    expect((10.d / 8.d).toString()).toEqual("1.25")
    expect((100.d / 97.d).toString()).toEqual("1.03")
    expect((100.d / 87.d).toString()).toEqual("1.15")
    expect((100.d % 87.d).toString()).toEqual("13.00")
    expect((10.05.d % 8.03.d).toString()).toEqual("2.02")
    expect("1.33".d.toString()).toEqual("1.33")
    expect("1.2".d.toString()).toEqual("1.20")
    expect("145".d.toString()).toEqual("145.00")
    expect(-10.d).toEqual("-10".d)
  }

  @Test fun `min & max`() {
    expect(Decimal.MAX_VALUE.toString()).toEqual("92233720368547758.07")
    expect(Decimal.MIN_VALUE.toString()).toEqual("-92233720368547758.08")
  }

  @Test fun `inc & dec`() {
    var x = 1.d
    expect(++x).toEqual(2.d)
    expect(--x).toEqual(1.d)
  }

  @Test fun abs() {
    expect(1.d.abs()).toEqual(1.d)
    expect((-1).d.abs()).toEqual(1.d)
  }

  @Test fun overflow() {
    expect { Decimal.MAX_VALUE * 2.d }.toThrow<ArithmeticException>()
    expect { Decimal.MAX_VALUE + 1.d }.toThrow<ArithmeticException>()
    expect { Decimal.MIN_VALUE - 1.d }.toThrow<ArithmeticException>()
  }

  @Test fun compareTo() {
    expect(1.d).toBeGreaterThan(0.5.d)
    expect(2.d).toBeLessThan(2.5.d)
    expect(1.d.compareTo(1.00.d)).toEqual(0)
  }

  @Test fun percent() {
    expect(200.d percent 20.d).toEqual(40.d)
  }

  @Test fun formatting() {
    expect(NumberFormat.getNumberInstance(Locale("et")).format(10500.24.d)).toEqual("10 500,24")
  }
}

import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThan
import ch.tutteli.atrium.api.fluent.en_GB.toBeLessThan
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import klite.Monetary
import klite.m
import org.junit.jupiter.api.Test

class MonetaryTest {
  @Test fun `operators & string`() {
    expect(20.m.toString()).toEqual("20.00")
    expect((3.m * 5.m).toString()).toEqual("15.00")
    expect((3.m / 5.m).toString()).toEqual("0.60")
    expect((10.m / 8.m).toString()).toEqual("1.25")
    expect((100.m / 97.m).toString()).toEqual("1.03")
    expect((100.m / 87.m).toString()).toEqual("1.15")
    expect((100.m % 87.m).toString()).toEqual("13.00")
    expect((10.05.m % 8.03.m).toString()).toEqual("2.02")
    expect("1.33".m.toString()).toEqual("1.33")
    expect("1.2".m.toString()).toEqual("1.20")
    expect("145".m.toString()).toEqual("145.00")
    expect(-10.m).toEqual("-10".m)
  }

  @Test fun `min & max`() {
    expect(Monetary.MAX_VALUE.toString()).toEqual("92233720368547758.07")
    expect(Monetary.MIN_VALUE.toString()).toEqual("-92233720368547758.08")
  }

  @Test fun `inc & dec`() {
    var x = 1.m
    expect(++x).toEqual(2.m)
    expect(--x).toEqual(1.m)
  }

  @Test fun overflow() {
    expect { Monetary.MAX_VALUE * 2.m }.toThrow<ArithmeticException>()
    expect { Monetary.MAX_VALUE + 1.m }.toThrow<ArithmeticException>()
    expect { Monetary.MIN_VALUE - 1.m }.toThrow<ArithmeticException>()
  }

  @Test fun compareTo() {
    expect(1.m).toBeGreaterThan(0.5.m)
    expect(2.m).toBeLessThan(2.5.m)
    expect(1.m.compareTo(1.00.m)).toEqual(0)
  }

  @Test fun percent() {
    expect(200.m percent 20.m).toEqual(40.m)
  }
}

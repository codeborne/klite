import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.m
import org.junit.jupiter.api.Test

class MonetaryTest {
  @Test fun string() {
    expect(20.m.toString()).toEqual("20.00")
    expect((3.m * 5.m).toString()).toEqual("15.00")
    expect((3.m / 5.m).toString()).toEqual("0.60")
    expect((10.m / 8.m).toString()).toEqual("1.25")
    expect((100.m / 97.m).toString()).toEqual("1.03")
    //expect((100.m / 87.m).toString()).toEqual("1.15")
    expect("1.33".m.toString()).toEqual("1.33")
    expect("1.2".m.toString()).toEqual("1.20")
    expect("145".m.toString()).toEqual("145.00")
  }
}

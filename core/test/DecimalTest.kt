import ch.tutteli.atrium.api.fluent.en_GB.notToEqual
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.d
import org.junit.jupiter.api.Test

class DecimalTest {
  @Test fun equals() {
    expect("1.00".d == 1.d).toEqual(true)
    expect("1.01".d != 1.d).toEqual(true)
    expect("1e3".d == 1000.d).toEqual(true)
  }

  @Test fun hashCodes() {
    expect("1.00".d.hashCode()).toEqual(1.d.hashCode())
    expect("1.01".d.hashCode()).notToEqual(1.d.hashCode())
  }

  @Test fun strings() {
    expect("3.00".d.toString()).toEqual("3.00")
    expect(3.d.toString()).toEqual("3")
    expect("1e3".d.toString()).toEqual("1000")
  }
}

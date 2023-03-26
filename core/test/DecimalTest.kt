import ch.tutteli.atrium.api.fluent.en_GB.notToEqual
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.d
import org.junit.jupiter.api.Test

class DecimalTest {
  @Test fun `equals & hashCode`() {
    expect("1.00".d == 1.d).toEqual(true)
    expect("1.00".d.hashCode()).toEqual(1.d.hashCode())

    expect("1.01".d != 1.d).toEqual(true)
    expect("1.01".d.hashCode()).notToEqual(1.d.hashCode())
  }
}

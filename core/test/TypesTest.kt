package klite

import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class TypesTest {
  @Test fun email() {
    val email = Email(" hello@DOmain.zz\n")
    expect(email.value).toEqual("hello@domain.zz")
    expect(email).toEqual(Email(email.value))
    expect(email.hashCode()).toEqual(email.value.hashCode())
    expect(email.domain).toEqual("domain.zz")
  }

  @Test fun `invalid email`() {
    expect { Email("blah") }.toThrow<IllegalArgumentException>().messageToContain("Invalid email: blah")
  }

  @Test fun phone() {
    expect(Phone(" +372 (56) 639-678").value).toEqual("+37256639678")
  }

  @Test fun `invalid phone`() {
    expect { Phone("blah") }.toThrow<IllegalArgumentException>()
  }
}

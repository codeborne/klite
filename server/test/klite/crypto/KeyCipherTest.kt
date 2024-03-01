package klite.crypto

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class KeyCipherTest {
  val keyGenerator = KeyGenerator()
  val keyCipher = KeyCipher(keyGenerator.keyFromSecret("my secret"))

  @Test fun `encrypt and decrypt`() {
    expect(keyCipher.encrypt("Hello")).toEqual("Toy5Uw8i-HGTYN49WJtarw")
    expect(keyCipher.decrypt("Toy5Uw8i-HGTYN49WJtarw")).toEqual("Hello")
    expect(keyCipher.decrypt("Toy5Uw8i+HGTYN49WJtarw==")).toEqual("Hello")
  }
}

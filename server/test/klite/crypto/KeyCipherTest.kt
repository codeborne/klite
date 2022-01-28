package klite.crypto

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class KeyCipherTest {
  val keyGenerator = KeyGenerator()
  val keyCipher = KeyCipher(keyGenerator.keyFromSecret("my secret"))

  @Test fun `encrypt and decrypt`() {
    expectThat(keyCipher.encrypt("Hello")).isEqualTo("Toy5Uw8i+HGTYN49WJtarw==")
    expectThat(keyCipher.decrypt("Toy5Uw8i+HGTYN49WJtarw==")).isEqualTo("Hello")
  }
}

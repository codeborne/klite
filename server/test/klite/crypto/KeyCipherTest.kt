package klite.crypto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KeyCipherTest {
  val keyGenerator = KeyGenerator()
  val keyCipher = KeyCipher(keyGenerator.keyFromSecret("my secret"))

  @Test fun `encrypt and decrypt`() {
    assertThat(keyCipher.encrypt("Hello")).isEqualTo("Toy5Uw8i+HGTYN49WJtarw==")
    assertThat(keyCipher.decrypt("Toy5Uw8i+HGTYN49WJtarw==")).isEqualTo("Hello")
  }
}

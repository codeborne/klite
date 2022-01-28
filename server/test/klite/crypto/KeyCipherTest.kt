package klite.crypto

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test

class KeyCipherTest {
  val keyGenerator = KeyGenerator()
  val keyCipher = KeyCipher(keyGenerator.keyFromSecret("my secret"))

  @Test fun `encrypt and decrypt`() {
    expect(keyCipher.encrypt("Hello")).to.equal("Toy5Uw8i+HGTYN49WJtarw==")
    expect(keyCipher.decrypt("Toy5Uw8i+HGTYN49WJtarw==")).to.equal("Hello")
  }
}

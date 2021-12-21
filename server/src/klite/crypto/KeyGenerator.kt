package klite.crypto

import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class KeyGenerator(val keyFactory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")) {
  fun hash(password: String, salt: String?): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt?.toByteArray(), 65536, 256)
    return keyFactory.generateSecret(spec).encoded
  }

  fun keyFromSecret(password: String, salt: String? = "SaltyKlite"): SecretKey {
    return SecretKeySpec(hash(password, salt), "AES")
  }
}

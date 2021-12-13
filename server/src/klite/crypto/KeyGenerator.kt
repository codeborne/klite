package klite.crypto

import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class KeyGenerator(val keyFactory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")) {
  fun keyFromSecret(password: String, salt: String? = "SaltyKlite"): SecretKey {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt?.toByteArray(), 65536, 256)
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
  }
}

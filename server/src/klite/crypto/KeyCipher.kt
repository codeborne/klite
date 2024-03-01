package klite.crypto

import klite.base64UrlDecode
import klite.base64UrlEncode
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.SecretKey

class KeyCipher(private val key: SecretKey) {
  private fun cipherFor(mode: Int) = Cipher.getInstance(key.algorithm).apply {
    init(mode, key)
  }

  fun encrypt(input: String): String =
    cipherFor(ENCRYPT_MODE).doFinal(input.toByteArray()).base64UrlEncode()

  fun decrypt(input: String): String =
    cipherFor(DECRYPT_MODE).doFinal(input.base64UrlDecode()).decodeToString()
}

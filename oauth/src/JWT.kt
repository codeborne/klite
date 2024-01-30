package klite.oauth

import klite.base64Decode

data class JWT(val token: String) {
  private val parts = token.split(".").map { it.base64Decode().decodeToString() }
  val header get() = parts[0]
  val payload get() = parts[1]
  val signature get() = parts[2]
}

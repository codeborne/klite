package klite.oauth

import klite.Email
import klite.base64UrlDecode
import klite.json.*
import java.time.Instant
import java.util.*

data class JWT(val token: String) {
  companion object {
    private val jsonMapper = JsonMapper()
  }

  private val parts = token.split(".").map { it.base64UrlDecode().decodeToString() }
  val headerJson get() = parts[0]
  val payloadJson get() = parts[1]
  val signature get() = parts[2]

  val header by lazy { Header(jsonMapper.parse<JsonNode>(headerJson)) }
  val payload by lazy { Payload(jsonMapper.parse<JsonNode>(payloadJson)) }

  data class Header(val fields: JsonNode): JsonNode by fields {
    val alg by fields
    val typ by fields
  }

  /** https://www.iana.org/assignments/jwt/jwt.xhtml#claims */
  data class Payload(val claims: JsonNode): JsonNode by claims {
    val subject get() = getString("sub")
    val audience get() = getString("aud")
    val issuedAt get() = getOrNull<Number>("iat")?.let { Instant.ofEpochSecond(it.toLong()) }
    val issuer = getOrNull<String>("iss")
    val expiresAt get() = getOrNull<Number>("exp")?.let { Instant.ofEpochSecond(it.toLong()) }
    val name get() = getOrNull<String>("name")
    val email get() = getOrNull<String>("email")?.let { Email(it) }
    val emailVerified get() = getOrNull<Boolean>("email_verified")
    val locale get() = getOrNull<String>("locale")?.let { Locale.forLanguageTag(it) }
  }

  data class Signature(val fields: JsonNode): JsonNode by fields
}

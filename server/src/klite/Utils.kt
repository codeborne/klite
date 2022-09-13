package klite

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Duration.ofDays
import java.util.*

fun String.urlDecode() = URLDecoder.decode(this, Charsets.UTF_8)!!
fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8)!!

fun ByteArray.base64Encode() = Base64.getEncoder().encodeToString(this)
fun String.base64Encode() = toByteArray().base64Encode()
fun String.base64Decode() = Base64.getDecoder().decode(this)

typealias Params = Map<String, String?>
val URI.queryParams: Params get() = urlDecodeParams(rawQuery)

fun urlEncodeParams(params: Map<String, Any?>) = params.mapNotNull { e -> e.value?.let { e.key + "=" + it.toString().urlEncode() } }.joinToString("&")
fun urlDecodeParams(params: String?): Params = params?.split('&')?.associate(::keyValue) ?: emptyMap()
internal fun keyValue(s: String) = s.split('=', limit = 2).let { it[0] to it.getOrNull(1)?.urlDecode() }

operator fun URI.plus(suffix: String) = URI(toString().substringBefore("#") + suffix + (fragment?.let { "#$it" } ?: ""))
operator fun URI.plus(params: Map<String, Any?>) = plus((if (rawQuery == null) "?" else "&") + urlEncodeParams(params))

fun String?.trimToNull() = this?.trim()?.takeIf { it.isNotEmpty() }

fun Server.enforceHttps(maxAgeSec: Long = ofDays(365).toSeconds()) = before { e ->
  if (!e.isSecure) {
    e.header("Strict-Transport-Security", "max-age=$maxAgeSec")
    e.redirect(e.fullUrl.toString().replace("http://", "https://"), StatusCode.PermanentRedirect)
  }
}

@Suppress("UNCHECKED_CAST")
fun <V> mapOfNotNull(vararg pairs: Pair<String, V?>) = mapOf(*pairs).filterValues { it != null } as Map<String, V>

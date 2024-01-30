package klite

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

fun String.urlDecode() = URLDecoder.decode(this, Charsets.UTF_8)!!
fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8)!!

fun ByteArray.base64Encode() = Base64.getEncoder().encodeToString(this)
fun String.base64Encode() = toByteArray().base64Encode()
fun String.base64UrlEncode() = base64Encode().replace('+', '-').replace('/', '_').trimEnd('=')
fun String.base64Decode() = Base64.getDecoder().decode(this)
fun String.base64UrlDecode() = replace('-', '+').replace('_', '/').base64Decode()

typealias Params = Map<String, String?>
val URI.queryParams: Params get() = urlDecodeParams(rawQuery)

fun urlEncodeParams(params: Map<String, Any?>) = params.mapNotNull { e -> e.value?.let { e.key + "=" + it.toString().urlEncode() } }.joinToString("&")
fun urlDecodeParams(params: String?): Params = params?.split('&')?.associate(::keyValue) ?: emptyMap()
internal fun keyValue(s: String) = s.split('=', limit = 2).let { it[0] to it.getOrNull(1)?.urlDecode() }

operator fun URI.plus(suffix: String) = URI(toString().substringBefore("#") + suffix + (fragment?.let { "#$it" } ?: ""))
operator fun URI.plus(params: Map<String, Any?>) = plus((if (rawQuery == null) "?" else "&") + urlEncodeParams(params))

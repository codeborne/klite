package klite

import java.lang.System.Logger
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

inline fun Logger.info(msg: String) = log(Logger.Level.INFO, msg)

fun String.urlDecode() = URLDecoder.decode(this, Charsets.UTF_8)!!
fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8)!!

fun urlDecodeParams(params: String?) = params?.split('&')?.associate {
  it.split('=', limit = 2).let { it[0] to it.getOrNull(1)?.urlDecode() }
} ?: emptyMap()

val URI.queryParams: Map<String, String?> get() = urlDecodeParams(rawQuery)

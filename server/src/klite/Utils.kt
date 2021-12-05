package klite

import java.lang.System.Logger
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

fun Any.logger(): Logger = System.getLogger(javaClass.name)
inline fun Logger.info(msg: String) = log(Logger.Level.INFO, msg)

fun String.urlDecode() = URLDecoder.decode(this, Charsets.UTF_8)!!
fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8)!!

typealias Params = Map<String, String?>

fun urlDecodeParams(params: String?): Params = params?.split('&')?.associate(::keyValue) ?: emptyMap()
internal fun keyValue(s: String) = s.split('=', limit = 2).let { it[0] to it.getOrNull(1)?.urlDecode() }

val URI.queryParams: Params get() = urlDecodeParams(rawQuery)

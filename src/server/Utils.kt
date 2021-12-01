package server

import java.lang.System.Logger
import java.net.URLDecoder
import java.net.URLEncoder

fun String.urlDecode() = URLDecoder.decode(this, Charsets.UTF_8)!!
fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8)!!

inline fun Logger.info(msg: String) = log(Logger.Level.INFO, msg)

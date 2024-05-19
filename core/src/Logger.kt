@file:Suppress("NOTHING_TO_INLINE")
package klite

import java.lang.System.Logger.Level.*

fun logger(name: String): System.Logger = System.getLogger(name)
fun Any.logger(): System.Logger = System.getLogger(javaClass.name)
inline fun System.Logger.debug(msg: String) = log(DEBUG, msg)
inline fun System.Logger.info(msg: String) = log(INFO, msg)
inline fun System.Logger.warn(msg: String) = log(WARNING, msg)
inline fun System.Logger.error(msg: String, e: Throwable? = null) = log(ERROR, msg, e)
inline fun System.Logger.error(e: Throwable) = log(ERROR, e.message ?: e.javaClass.name, e)

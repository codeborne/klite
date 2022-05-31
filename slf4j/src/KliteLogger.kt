package klite.slf4j

import klite.Config
import org.slf4j.event.Level
import org.slf4j.event.Level.*
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter.arrayFormat
import org.slf4j.helpers.MessageFormatter.format
import java.lang.System.currentTimeMillis
import java.lang.Thread.currentThread

open class KliteLogger(name: String): MarkerIgnoringBase() {
  companion object {
    internal var out = System.out
    private val defaultLevel = Config.optional("LOGGER_LEVEL", "INFO")
    private val start = currentTimeMillis()
  }

  open val shortName = name.substringAfterLast(".")
  open val level = Level.valueOf(Config.optional("LOGGER.$name", defaultLevel))
  init { this.name = name }

  override fun isTraceEnabled() = level >= TRACE
  override fun isDebugEnabled() = level >= DEBUG
  override fun isInfoEnabled() = level >= INFO
  override fun isWarnEnabled() = level >= WARN
  override fun isErrorEnabled() = level >= ERROR

  override fun trace(msg: String?) = log(TRACE, msg)
  override fun trace(msg: String?, t: Throwable?) = log(TRACE, msg, t)
  override fun trace(format: String, arg: Any?) = log(TRACE, format, arg)
  override fun trace(format: String, arg1: Any?, arg2: Any?) = log(TRACE, format, arg1, arg2)
  override fun trace(format: String, vararg args: Any?) = log(TRACE, format, *args)

  override fun debug(msg: String?) = log(DEBUG, msg)
  override fun debug(msg: String?, t: Throwable?) = log(DEBUG, msg, t)
  override fun debug(format: String, arg: Any?) = log(DEBUG, format, arg)
  override fun debug(format: String, arg1: Any?, arg2: Any?) = log(DEBUG, format, arg1, arg2)
  override fun debug(format: String, vararg args: Any?) = log(DEBUG, format, *args)

  override fun info(msg: String?) = log(INFO, msg)
  override fun info(msg: String?, t: Throwable?) = log(INFO, msg, t)
  override fun info(format: String, arg: Any?) = log(INFO, format, arg)
  override fun info(format: String, arg1: Any?, arg2: Any?) = log(INFO, format, arg1, arg2)
  override fun info(format: String, vararg args: Any?) = log(INFO, format, *args)

  override fun warn(msg: String?) = log(WARN, msg)
  override fun warn(msg: String?, t: Throwable?) = log(WARN, msg, t)
  override fun warn(format: String, arg: Any?) = log(WARN, format, arg)
  override fun warn(format: String, arg1: Any?, arg2: Any?) = log(WARN, format, arg1, arg2)
  override fun warn(format: String, vararg args: Any?) = log(WARN, format, *args)

  override fun error(msg: String?) = log(ERROR, msg)
  override fun error(msg: String?, t: Throwable?) = log(ERROR, msg, t)
  override fun error(format: String, arg: Any?) = log(ERROR, format, arg)
  override fun error(format: String, arg1: Any?, arg2: Any?) = log(ERROR, format, arg1, arg2)
  override fun error(format: String, vararg args: Any?) = log(ERROR, format, *args)

  protected open fun log(level: Level, msg: String?, t: Throwable? = null) {
    if (this.level < level) return
    out.println("${currentTimeMillis() - start} [${currentThread().name}] $level $shortName - ${msg ?: ""}")
    t?.printStackTrace(out)
  }

  protected fun log(level: Level, format: String, vararg args: Any?) {
    if (this.level < level) return
    arrayFormat(format, args).apply { log(level, msg = message, throwable) }
  }

  protected fun log(level: Level, format: String, arg: Any?) {
    if (this.level < level) return
    format(format, arg).apply { log(level, msg = message, throwable) }
  }
}

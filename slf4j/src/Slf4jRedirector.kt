package klite.slf4j

import org.slf4j.LoggerFactory
import java.lang.System.Logger.Level.*
import java.text.MessageFormat
import java.util.*

class Slf4jRedirector(name: String): System.Logger {
  private val slf4j = LoggerFactory.getLogger(name)

  override fun getName(): String = slf4j.name

  override fun isLoggable(level: System.Logger.Level) = when(level) {
    ALL, TRACE -> slf4j.isTraceEnabled
    DEBUG -> slf4j.isDebugEnabled
    INFO -> slf4j.isInfoEnabled
    WARNING -> slf4j.isWarnEnabled
    ERROR -> slf4j.isErrorEnabled
    OFF -> false
  }

  override fun log(level: System.Logger.Level, bundle: ResourceBundle?, msg: String?, thrown: Throwable?) = when(level) {
    ALL, TRACE -> slf4j.trace(msg, thrown)
    DEBUG -> slf4j.debug(msg, thrown)
    INFO -> slf4j.info(msg, thrown)
    WARNING -> slf4j.warn(msg, thrown)
    ERROR -> slf4j.error(msg, thrown)
    OFF -> Unit
  }

  override fun log(level: System.Logger.Level, bundle: ResourceBundle?, format: String?, params: Array<Any?>?) =
    log(level, bundle, if (params == null) format else MessageFormat.format(format, params), null as Throwable?)
}


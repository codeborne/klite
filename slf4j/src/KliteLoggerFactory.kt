package klite.slf4j

import klite.Config
import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap

class KliteLoggerFactory: ILoggerFactory {
  private val loggers = ConcurrentHashMap<String, Logger>()
  private val loggerConstructor by lazy { findConstructor(Config.optional("LOGGER_CLASS", KliteLogger::class.java.name)) }
  @Suppress("UNCHECKED_CAST") internal fun findConstructor(className: String) = Class.forName(className).getConstructor(String::class.java) as Constructor<Logger>
  override fun getLogger(name: String) = loggers.getOrPut(name) { loggerConstructor.newInstance(name) }
}

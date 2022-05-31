package klite.slf4j

import klite.Config
import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

class KliteLoggerFactory: ILoggerFactory {
  private val loggers = ConcurrentHashMap<String, Logger>()
  private val loggerConstructor = findConstructor(Config.optional("LOGGER_CLASS", KliteLogger::class.qualifiedName!!))
  @Suppress("UNCHECKED_CAST") internal fun findConstructor(className: String) = Class.forName(className).kotlin.primaryConstructor as KFunction<Logger>
  override fun getLogger(name: String) = loggers.getOrPut(name) { loggerConstructor.call(name) }
}

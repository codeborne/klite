package klite.slf4j

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap

class KliteLoggerFactory: ILoggerFactory {
  private val loggers = ConcurrentHashMap<String, Logger>()
  override fun getLogger(name: String) = loggers.getOrPut(name) { KliteLogger(name) }
}

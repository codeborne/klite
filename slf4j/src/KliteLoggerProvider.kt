package klite.slf4j

import org.slf4j.helpers.NOP_FallbackServiceProvider

class KliteLoggerProvider: NOP_FallbackServiceProvider() {
  private val factory = KliteLoggerFactory()
  override fun getLoggerFactory() = factory
}

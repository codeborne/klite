package klite

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

typealias RequestLogFormatter = HttpExchange.(ms: Long) -> String

open class RequestLogger(
  val formatter: RequestLogFormatter = { ms -> "$remoteAddress $method $path$query: $statusCode in $ms ms" }
): Decorator {
  companion object {
    val prefix = (0xFFFF * Math.random()).toInt().toString(16)
  }

  private val logger = logger()
  private val counter = AtomicLong()

  init {
    logger.info("Starting node: $prefix")
  }

  override suspend fun invoke(exchange: HttpExchange, handler: Handler): Any? {
    val start = System.nanoTime()
    val requestId = "$prefix-${counter.incrementAndGet()}" + (exchange.header("X-Request-Id")?.let { "/$it" } ?: "")
    exchange.onComplete {
      val ms = (System.nanoTime() - start) / 1000_000
      logger.info(formatter(exchange, ms))
    }
    return withContext(RequestThreadNameContext(requestId)) {
      handler(exchange)
    }
  }
}

class RequestThreadNameContext(private val requestId: String): ThreadContextElement<String?>, AbstractCoroutineContextElement(Key) {
  companion object Key: CoroutineContext.Key<RequestThreadNameContext>

  override fun updateThreadContext(context: CoroutineContext) = Thread.currentThread().also { it.name = requestId }.name
  override fun restoreThreadContext(context: CoroutineContext, oldState: String?) { Thread.currentThread().name = oldState }
}

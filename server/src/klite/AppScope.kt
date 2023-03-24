package klite

import kotlinx.coroutines.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

object AppScope: CoroutineScope {
  private val exceptionHandler = CoroutineExceptionHandler { _, e -> logger().error("Async operation failed", e) }
  private val fixedContext = SupervisorJob() + exceptionHandler + NonCancellable
  override val coroutineContext get() = fixedContext + ThreadNameContext(Thread.currentThread().name + "+async")
}

class ThreadNameContext(private val requestId: String): ThreadContextElement<String?>, AbstractCoroutineContextElement(Key) {
  companion object Key: CoroutineContext.Key<ThreadNameContext>
  override fun updateThreadContext(context: CoroutineContext) = Thread.currentThread().also { it.name = requestId }.name
  override fun restoreThreadContext(context: CoroutineContext, oldState: String?) { Thread.currentThread().name = oldState }
}

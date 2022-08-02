package klite

import kotlinx.coroutines.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

object AppScope: CoroutineScope {
  private val exceptionHandler = CoroutineExceptionHandler { _, e -> logger().error("Async operation failed", e) }
  override val coroutineContext get() = exceptionHandler + NonCancellable

  fun async(block: suspend CoroutineScope.() -> Unit) = launch(ThreadNameContext(Thread.currentThread().name + "+async"), block = block)
}

class ThreadNameContext(private val requestId: String): ThreadContextElement<String?>, AbstractCoroutineContextElement(Key) {
  companion object Key: CoroutineContext.Key<ThreadNameContext>
  override fun updateThreadContext(context: CoroutineContext) = Thread.currentThread().also { it.name = requestId }.name
  override fun restoreThreadContext(context: CoroutineContext, oldState: String?) { Thread.currentThread().name = oldState }
}

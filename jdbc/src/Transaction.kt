package klite.jdbc

import klite.error
import klite.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import java.sql.Connection
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class Transaction(private val db: DataSource) {
  companion object {
    private val threadLocal = ThreadLocal<Transaction>()
    fun current(): Transaction? = threadLocal.get()
  }

  private var conn: Connection? = null
  val connection: Connection
    get() = conn ?: db.connection.also { it.autoCommit = false; conn = it }

  fun close(commit: Boolean = true) {
    try {
      conn?.apply {
        if (!autoCommit) {
          if (commit) commit() else rollback()
          autoCommit = true
        }
        close()
      }
    } finally {
      conn = null
      detachFromThread()
    }
  }

  fun commit() = conn?.commit()
  fun rollback() = conn?.rollback()

  fun attachToThread() = this.also { threadLocal.set(it) }
  fun detachFromThread() = threadLocal.remove()
}

fun <R> DataSource.withConnection(block: Connection.() -> R): R {
  val tx = Transaction.current()
  return if (tx != null) tx.connection.block()
         else connection.use(block)
}

class TransactionContext(val tx: Transaction? = Transaction.current()): ThreadContextElement<Transaction?>, AbstractCoroutineContextElement(Key) {
  companion object Key: CoroutineContext.Key<TransactionContext>

  override fun updateThreadContext(context: CoroutineContext) = Transaction.current().also { tx?.attachToThread() }
  override fun restoreThreadContext(context: CoroutineContext, oldState: Transaction?) { oldState?.attachToThread() ?: tx?.detachFromThread() }
}

object AppScope: CoroutineScope {
  private val exceptionHandler = CoroutineExceptionHandler { _, e -> logger().error("Async operation failed", e) }
  override val coroutineContext get() = exceptionHandler + NonCancellable
}

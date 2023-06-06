package klite.jdbc

import klite.error
import klite.logger
import kotlinx.coroutines.ThreadContextElement
import java.sql.Connection
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class Transaction(private val db: DataSource): AutoCloseable {
  companion object {
    private val log = logger()
    private val threadLocal = ThreadLocal<Transaction>()
    fun current(): Transaction? = threadLocal.get()
  }

  private var conn: Connection? = null
  val connection: Connection get() = conn ?: openConnection()

  private fun openConnection() = db.connection.apply {
    autoCommit = false
    conn = this
  }

  override fun close() = close(true)
  fun close(commit: Boolean = true) {
    try {
      conn?.apply {
        if (!autoCommit) {
          if (commit) commit() else rollback()
          autoCommit = true
        }
      }
    } finally {
      try { conn?.close() } catch (e: Exception) { log.error("Failed to close $conn: $e") }
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

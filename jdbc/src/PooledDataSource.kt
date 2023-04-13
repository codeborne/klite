package klite.jdbc

import klite.Config
import klite.info
import klite.logger
import java.sql.Connection
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** EXPERIMENTAL */
internal class PooledDataSource(
  val db: DataSource,
  val size: Int = Config.optional("DB_POOL_SIZE")?.toInt() ?: ((Config.optional("NUM_WORKERS")?.toInt() ?: 5) + (Config.optional("JOB_WORKERS")?.toInt() ?: 5)),
  val timeout: Duration = 5.seconds
): DataSource by db, AutoCloseable {
  private val log = logger()
  val pool = ArrayBlockingQueue<PooledConnection>(size)
  val used = ArrayBlockingQueue<PooledConnection>(size)

  override fun getConnection(): PooledConnection = pool.poll()?.check() ?:
    (if (pool.remainingCapacity() == 0) pool.poll(timeout.inWholeMilliseconds, MILLISECONDS)?.check() else null) ?:
    PooledConnection(db.connection).also { used.put(it) }

  override fun close() {
    pool.removeIf { it.conn.close(); true }
    used.removeIf { it.conn.close(); true }
  }

  override fun isWrapperFor(iface: Class<*>) = db::class.java.isAssignableFrom(iface)
  override fun <T> unwrap(iface: Class<T>): T? = if (isWrapperFor(iface)) db as T else null

  inner class PooledConnection(val conn: Connection): Connection by conn {
    init { log.info("New connection: $conn") }

    fun check() = if (isValid(timeout.inWholeSeconds.toInt())) this else null

    override fun close() {
      if (!conn.autoCommit) conn.rollback()
      used -= this
      pool += this
    }

    override fun isWrapperFor(iface: Class<*>) = conn::class.java.isAssignableFrom(iface)
    override fun <T> unwrap(iface: Class<T>): T? = if (isWrapperFor(iface)) conn as T else null
  }
}

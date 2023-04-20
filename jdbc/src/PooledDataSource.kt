package klite.jdbc

import klite.Config
import klite.info
import klite.logger
import klite.warn
import java.lang.System.currentTimeMillis
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Deprecated("EXPERIMENTAL") @Suppress("UNCHECKED_CAST")
class PooledDataSource(
  val db: DataSource,
  val maxSize: Int = Config.optional("DB_POOL_SIZE")?.toInt() ?: ((Config.optional("NUM_WORKERS")?.toInt() ?: 5) + (Config.optional("JOB_WORKERS")?.toInt() ?: 5)),
  val timeout: Duration = 5.seconds
): DataSource by db, AutoCloseable {
  private val log = logger()
  val size = AtomicInteger()
  val pool = ArrayBlockingQueue<PooledConnection>(maxSize)
  val used = ArrayBlockingQueue<PooledConnection>(maxSize)

  override fun getConnection(): PooledConnection {
    var conn: PooledConnection?
    do {
      conn = pool.poll()
      if (conn != null && !conn.check()) {
        log.warn("Dropping failed $conn, age ${conn.ageMs} ms")
        size.decrementAndGet()
        conn = null
        continue
      }
      if (conn == null) {
        conn = if (size.incrementAndGet() <= maxSize)
          PooledConnection(db.connection)
        else {
          size.decrementAndGet()
          pool.poll(timeout.inWholeMilliseconds, MILLISECONDS)
        }
      }
    } while (conn == null)
    used += conn
    return conn
  }

  override fun close() {
    pool.removeIf { it.reallyClose(); true }
    used.removeIf { it.reallyClose(); true }
  }

  inner class PooledConnection(val conn: Connection): Connection by conn {
    val since = currentTimeMillis()
    init { log.info("New connection: $this") }

    val ageMs get() = currentTimeMillis() - since

    fun check() = isValid(timeout.inWholeSeconds.toInt())

    override fun close() {
      if (!conn.autoCommit) conn.rollback()
      used -= this
      pool += this
    }

    fun reallyClose() = try {
      log.info("Closing $conn, age $ageMs ms")
      conn.close()
    } catch (e: SQLException) {
      log.warn("Failed to close $conn: $e")
    }

    override fun toString() = "PooledConnection:" + hashCode().toString(36) + ":" + conn

    override fun isWrapperFor(iface: Class<*>) = conn::class.java.isAssignableFrom(iface)
    override fun <T> unwrap(iface: Class<T>): T? = if (isWrapperFor(iface)) conn as T else null
  }

  override fun isWrapperFor(iface: Class<*>) = db::class.java.isAssignableFrom(iface)
  override fun <T> unwrap(iface: Class<T>): T? = if (isWrapperFor(iface)) db as T else null
}

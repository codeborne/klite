package klite.jdbc

import klite.*
import java.lang.System.currentTimeMillis
import java.lang.Thread.currentThread
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Deprecated("EXPERIMENTAL") @Suppress("UNCHECKED_CAST")
class PooledDataSource(
  val db: DataSource = ConfigDataSource(),
  val maxSize: Int = Config.dbPoolMaxSize,
  val timeout: Duration = 5.seconds,
  val leakCheckThreshold: Duration? = null
): DataSource by db, AutoCloseable {
  private val log = logger()
  val counter = AtomicInteger()
  val size = AtomicInteger()
  val pool = ArrayBlockingQueue<PooledConnection>(maxSize)
  val used = ConcurrentHashMap<PooledConnection, Used>(maxSize)

  data class Used(val since: Long = currentTimeMillis(), val threadName: String = currentThread().name)

  private val leakChecker = leakCheckThreshold?.let {
    thread(name = "$this:leakChecker", isDaemon = true) {
      while (!Thread.interrupted()) {
        val now = currentTimeMillis()
        used.forEach { (conn, used) ->
          val usedFor = now - used.since
          if (usedFor >= it.inWholeMilliseconds)
            log.error("Possible leaked $conn, used for $usedFor ms, acquired by ${used.threadName}")
        }
        try { Thread.sleep(it.inWholeMilliseconds / 10) } catch (e: InterruptedException) { break }
      }
    }
  }

  override fun getConnection(username: String?, password: String?) = throw UnsupportedOperationException("Please use getConnection()")

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
          // TODO: check
        }
      }
    } while (conn == null)
    used[conn] = Used()
    return conn
  }

  override fun close() {
    leakChecker?.interrupt()
    pool.removeIf { it.reallyClose(); true }
    used.keys.removeIf { it.reallyClose(); true }
  }

  inner class PooledConnection(private val conn: Connection): Connection by conn {
    val count = counter.incrementAndGet()
    val since = currentTimeMillis()
    init {
      log.info("New connection: $this")
      try { setNetworkTimeout(null, timeout.inWholeMilliseconds.toInt()) }
      catch (e: SQLException) { log.warn("Failed to set network timeout for $this: $e") }
    }

    val ageMs get() = currentTimeMillis() - since

    fun check(): Boolean {
      if (isClosed) return false
      return runCatching { applicationName = currentThread().name; true }.getOrNull() ?: false
    }

    override fun close() {
      if (!conn.autoCommit) conn.rollback()
      used -= this
      pool += this
    }

    fun reallyClose() = try {
      log.info("Closing $this, age $ageMs ms")
      conn.close()
    } catch (e: SQLException) {
      log.warn("Failed to close $this: $e")
    }

    override fun toString() = "Pooled#${count}:$conn"

    override fun isWrapperFor(iface: Class<*>) = conn::class.java.isAssignableFrom(iface)
    override fun <T> unwrap(iface: Class<T>): T? = if (isWrapperFor(iface)) conn as T else null
  }

  override fun isWrapperFor(iface: Class<*>) = db::class.java.isAssignableFrom(iface)
  override fun <T> unwrap(iface: Class<T>): T? = if (isWrapperFor(iface)) db as T else null
}

package klite.jdbc

import klite.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import java.sql.Connection
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PostgresNotifier<K: Any>(val channels: Map<K, Channel<String>>): Extension {
  constructor(channels: Iterable<K>): this(channels.associateWith { Channel<String>(UNLIMITED) })
  private lateinit var db: DataSource

  fun send(channel: K, payload: String = "") = db.notify(channel.toString(), payload)
  suspend fun receive(channel: K) = channels[channel]!!.receive()

  override fun install(server: Server) = server.run {
    db = require<DataSource>()
    val listener = thread(name = this::class.simpleName) {
      db.readNotificationsLoop(channels.mapKeys { it.toString() })
    }
    register(this)
    server.onStop { listener.interrupt() }
  }
}

/** Send Postgres notification to the specified channel. Delivered after commit */
fun DataSource.notify(channel: String, payload: String = "") = withStatement("select pg_notify(?, ?)") {
  setAll(sequenceOf(channel, payload))
  executeQuery().run { next() }
}

/** Dedicate a separate thread to listen to Postgres notifications and send them to the corresponding channels. */
fun DataSource.readNotificationsLoop(channels: Map<String, Channel<String>>, timeout: Duration = 10.seconds) = withConnection {
  listen(channels)
  while (!Thread.interrupted()) {
    pgNotifications(timeout).forEach {
      channels[it.name]?.trySend(it.parameter) ?: logger().warn("No channel for ${it.name} notification")
    }
  }
}

fun Connection.listen(channels: Map<String, Channel<String>>) = createStatement().use { s ->
  channels.keys.forEach { s.execute("listen $it") }
}

fun Connection.pgNotifications(timeout: Duration): Array<PGNotification> =
  unwrap(PGConnection::class.java).getNotifications(timeout.inWholeMilliseconds.toInt())

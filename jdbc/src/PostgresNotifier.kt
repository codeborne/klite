package klite.jdbc

import klite.Extension
import klite.Server
import klite.register
import klite.require
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import java.sql.Connection
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PostgresNotifier<K: Any>(val events: Iterable<K>): Extension {
  private val channels = events.associate { it.toString() to Channel<String>(UNLIMITED) }
  private lateinit var db: DataSource

  fun send(event: K, payload: String = "") = db.notify(event.toString(), payload)
  suspend fun receive(event: K) = channels[event.toString()]!!.receive()

  override fun install(server: Server) = server.run {
    db = require<DataSource>()
    val listener = thread(name = this::class.simpleName) {
      db.consumeNotifications(events.map { it.toString() }) {
        channels[it.name]?.trySend(it.parameter)
      }
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
fun DataSource.consumeNotifications(events: Iterable<String>, timeout: Duration = 10.seconds, consumer: (notification: PGNotification) -> Unit) = withConnection {
  listen(events)
  while (!Thread.interrupted()) {
    pgNotifications(timeout).forEach { consumer(it) }
  }
}

fun Connection.listen(events: Iterable<String>) = createStatement().use { s ->
  events.forEach { s.execute("listen $it") }
}

fun Connection.pgNotifications(timeout: Duration): Array<PGNotification> =
  unwrap(PGConnection::class.java).getNotifications(timeout.inWholeMilliseconds.toInt())

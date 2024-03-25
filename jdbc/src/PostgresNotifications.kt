package klite.jdbc

import klite.logger
import klite.warn
import kotlinx.coroutines.channels.Channel
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import java.sql.Connection
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Send Postgres notification to the specified channel. Delivered after commit */
fun DataSource.notify(channel: String, payload: String = "") = withStatement("select pg_notify(?, ?)") {
  setAll(sequenceOf(channel, payload))
  executeQuery().run { next() }
}

/** Dedicate a separate thread to listen to Postgres notifications and send them to the corresponding channels. */
fun DataSource.readNotificationsLoop(channels: Map<String, Channel<String?>>, timeout: Duration = 10.seconds) = withConnection {
  listen(channels)
  while (!Thread.interrupted()) {
    pgNotifications(timeout).forEach {
      channels[it.name]?.trySend(it.parameter) ?: logger().warn("No channel for ${it.name} notification")
    }
  }
}

fun Connection.listen(channels: Map<String, Channel<String?>>) = createStatement().use { s ->
  channels.keys.forEach { s.execute("listen $it") }
}

fun Connection.pgNotifications(timeout: Duration): Array<PGNotification> =
  unwrap(PGConnection::class.java).getNotifications(timeout.inWholeMilliseconds.toInt())

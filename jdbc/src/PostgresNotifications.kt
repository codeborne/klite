package klite.jdbc

import kotlinx.coroutines.channels.Channel
import org.postgresql.PGConnection
import javax.sql.DataSource

fun DataSource.sendNotification(channel: String, payload: String = "") = withStatement("select pg_notify(?, ?)") {
  setAll(sequenceOf(channel, payload))
  executeQuery().run { next() }
}

fun DataSource.readNotificationsLoop(channels: Map<String, Channel<String?>>, checkTimeoutMs: Int = 10000) = withConnection {
  createStatement().use { s ->
    channels.keys.forEach { s.execute("listen $it") }
  }
  val pgConn = unwrap(PGConnection::class.java)
  while (!Thread.interrupted()) {
    pgConn.getNotifications(checkTimeoutMs)?.forEach {
      channels[it.name]!!.trySend(it.parameter)
    }
  }
}

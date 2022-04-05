package klite.jdbc

import klite.Extension
import klite.Server
import klite.require
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

class RequestTransactionHandler(private val connectionCallback: ((Connection) -> Unit)? = null): Extension {
  override fun install(server: Server) = server.run {
    val db = require<DataSource>()

    decorator { exchange, handler ->
      val tx = Transaction(db, connectionCallback)
      withContext(TransactionContext(tx)) {
        try {
          handler(exchange).also {
            tx.close(commit = true)
          }
        } catch (e: Exception) {
          tx.close(commit = false)
          throw e
        }
      }
    }
  }
}

package klite.jdbc

import klite.Extension
import klite.Server
import klite.require
import kotlinx.coroutines.withContext
import javax.sql.DataSource

annotation class NoTransaction

class RequestTransactionHandler: Extension {
  override fun install(server: Server) = server.run {
    val db = require<DataSource>()

    decorator { exchange, handler ->
      if (exchange.route.hasAnnotation<NoTransaction>())
        return@decorator handler(exchange)

      val tx = Transaction(db)
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

package klite.jdbc

import klite.*
import kotlinx.coroutines.withContext
import javax.sql.DataSource

annotation class NoTransaction

/**
 * Will start and close DB transaction for each request.
 * Normal finish or StatusCodeException will commit, any other Exception will rollback.
 */
class RequestTransactionHandler: Extension {
  override fun install(server: Server) = server.run {
    val db = require<DataSource>()
    decorator { exchange, handler -> decorate(db, exchange, handler) }
  }

  suspend fun decorate(db: DataSource, e: HttpExchange, handler: Handler): Any? {
    if (e.route.hasAnnotation<NoTransaction>()) return handler(e)

    val tx = Transaction(db)
    return withContext(TransactionContext(tx)) {
      try {
        handler(e).also {
          tx.close(commit = true)
        }
      } catch (e: Throwable) {
        tx.close(commit = e is StatusCodeException)
        throw e
      }
    }
  }
}

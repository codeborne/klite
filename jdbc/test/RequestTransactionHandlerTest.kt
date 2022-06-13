import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.HttpExchange
import klite.StatusCode
import klite.StatusCode.Companion.Found
import klite.StatusCodeException
import klite.jdbc.NoTransaction
import klite.jdbc.RequestTransactionHandler
import klite.jdbc.Transaction
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class RequestTransactionHandlerTest {
  val db = mockk<DataSource>(relaxed = true)
  val exchange = mockk<HttpExchange>(relaxed = true) {
    every { route.annotation(NoTransaction::class) } returns null
  }
  val txHandler = RequestTransactionHandler()

  @Test fun `commit on success`() {
    runBlocking {
      expect(txHandler.decorate(db, exchange) { Transaction.current()!!.connection }).toEqual(db.connection)
      expect(Transaction.current()).toEqual(null)
    }
    verify { db.connection.commit() }
  }

  @Test fun `commit on redirect`() {
    expect {
      runBlocking {
        txHandler.decorate(db, exchange) {
          Transaction.current()!!.connection
          throw StatusCodeException(Found, "http://www")
        }
      }
    }.toThrow<StatusCodeException>()
    expect(Transaction.current()).toEqual(null)
    verify { db.connection.commit() }
  }

  @Test fun `rollback on error`() {
    expect {
      runBlocking {
        txHandler.decorate(db, exchange) {
          Transaction.current()!!.connection
          error("Kaboom")
        }
      }
    }.toThrow<IllegalStateException>()
    expect(Transaction.current()).toEqual(null)
    verify { db.connection.rollback() }
  }
}

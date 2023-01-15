package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeTheInstance
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

class TransactionContextTest {
  val context = TransactionContext(Transaction(mockk()))

  @Test fun `transaction is attached and detached from thread`() {
    runBlocking {
      expect(Transaction.current()).toEqual(null)
      withContext(context) {
        expect(Transaction.current()).toBeTheInstance(context.tx)
      }
      expect(Transaction.current()).toEqual(null)
    }
  }

  @Test fun `nested transactions`() {
    runBlocking {
      withContext(context) {
        expect(Transaction.current()).toBeTheInstance(context.tx)

        val tx2 = Transaction(mockk())
        withContext(TransactionContext(tx2)) {
          expect(Transaction.current()).toBeTheInstance(tx2)
        }
        expect(Transaction.current()).toBeTheInstance(context.tx)
      }
      expect(Transaction.current()).toEqual(null)
    }
  }
}

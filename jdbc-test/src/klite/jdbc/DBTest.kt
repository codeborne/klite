package klite.jdbc

import klite.Config
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

/**
 * Base class for DB-dependent unit tests.
 * Will try to start dev DB via docker-compose automatically.
 */
abstract class DBTest {
  companion object {
    init {
      Config.useEnvFile()
      Config["ENV"] = "test"
      startDevDB()
    }

    val db: DataSource by lazy { DBModule().dataSource }
  }

  @BeforeEach open fun startTx() {
    Transaction(db).attachToThread()
  }

  @AfterEach open fun rollbackTx() {
    Transaction.current()!!.close(commit = false)
  }
}

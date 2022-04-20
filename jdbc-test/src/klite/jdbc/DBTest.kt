package klite.jdbc

import com.zaxxer.hikari.pool.HikariPool
import klite.Config
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import javax.sql.DataSource

abstract class DBTest {
  companion object {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)

    init {
      Config.useEnvFile()
      Config["ENV"] = "test"
    }

    val db: DataSource by lazy {
      try { DBModule().dataSource }
      catch (e: HikariPool.PoolInitializationException) {
        error("Test DB not running, please use `docker-compose up -d db`\n${e.message}")
      }
    }
  }

  @RegisterExtension @JvmField @Suppress("unused")
  val autoRollback = InTransactionRunner()

  class InTransactionRunner: BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext?) {
      Transaction(db).attachToThread()
    }

    override fun afterEach(context: ExtensionContext?) {
      Transaction.current()!!.close(commit = false)
    }
  }
}

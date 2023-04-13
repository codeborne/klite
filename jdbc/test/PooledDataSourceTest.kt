package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toHaveSize
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.mockk
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.sql.Connection
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds

class PooledDataSourceTest {
  val db = mockk<DataSource>(relaxed = true)
  internal val pooled = PooledDataSource(db, size = 3, timeout = 10.milliseconds)

  @Test fun pool() {
    val conns = (1..3).map { pooled.connection }
    expect(pooled.used).toHaveSize(3)
    expect(pooled.pool).toHaveSize(0)

    val extra = GlobalScope.async { pooled.connection }
    expect(extra.isActive).toEqual(true)
    expect(extra.isCompleted).toEqual(false)
    conns.forEach { it.close() }

    expect(pooled.used).toHaveSize(0)
    expect(pooled.pool).toHaveSize(3)
    expect(runBlocking { extra.await() }).toBeAnInstanceOf<Connection>()
    pooled.close()
    expect(pooled.pool).toHaveSize(0)
  }
}

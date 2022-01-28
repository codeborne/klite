package klite.jobs

import io.mockk.mockk
import io.mockk.verify
import klite.Server
import klite.jdbc.Transaction
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beNull
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource

class JobRunnerTest {
  val db = mockk<DataSource>()
  val executor = mockk<ScheduledExecutorService>(relaxed = true)
  val runner = JobRunner(db, executor)

  @Test fun `adds shutdown handler`() {
    val server = mockk<Server>(relaxed = true)
    runner.install(server)
    verify { server.onStop(any()) }
  }

  @Test fun runInTransaction() {
    val job = Job {
      expect(Transaction.current()).to.not.beNull()
    }
    runner.runInTransaction("My job", job)
  }

  @Test fun schedule() {
    val job = mockk<Job>()
    runner.schedule(job, 10, 20, SECONDS)
    verify { executor.scheduleAtFixedRate(any(), 10, 20, SECONDS) }
  }

  @Test fun `scheduleDaily with delay`() {
    val job = mockk<Job>()
    runner.scheduleDaily(job, delayMinutes = 10)
    verify { executor.scheduleAtFixedRate(any(), 10, 24 * 60, MINUTES) }
  }

  @Test fun `scheduleDaily at time`() {
    val job = mockk<Job>()
    runner.scheduleDaily(job, at = LocalTime.of(6, 30))
    verify { executor.scheduleAtFixedRate(any(), match { it > 0 }, 24 * 60, MINUTES) }
  }
}

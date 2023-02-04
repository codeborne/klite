package klite.jobs

import ch.tutteli.atrium.api.fluent.en_GB.notToEqualNull
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.*
import klite.RequestIdGenerator
import klite.Server
import klite.jdbc.Transaction
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource

class JobRunnerTest {
  val db = mockk<DataSource>()
  val executor = mockk<ScheduledExecutorService>(relaxed = true)
  val job = MyJob()
  val runner = JobRunner(db, RequestIdGenerator(), executor)

  class MyJob: Job {
    override val allowParallelRun = true
    override suspend fun run() {
      expect(Transaction.current()).notToEqualNull()
    }
  }

  @Test fun `adds shutdown handler`() {
    val server = mockk<Server>(relaxed = true)
    runner.install(server)
    verify { server.onStop(any()) }
  }

  @Test fun runInTransaction() {
    runner.runInTransaction(job)
  }

  @Test fun schedule() {
    runner.schedule(job, 10, 20, SECONDS)
    verify { executor.scheduleAtFixedRate(any(), 10, 20, SECONDS) }
  }

  @Test fun `scheduleDaily with delay`() {
    runner.scheduleDaily(job, delayMinutes = 10)
    verify { executor.scheduleAtFixedRate(any(), 10, 24 * 60, MINUTES) }
  }

  @Test fun `scheduleDaily at time`() {
    runner.scheduleDaily(job, LocalTime.of(6, 30), LocalTime.of(6, 45))
    verify(exactly = 2) { executor.scheduleAtFixedRate(any(), match { it > 0 }, 24 * 60, MINUTES) }
  }

  @Test fun `scheduleMonthly today`() {
    val job = spyk(job)
    runner.scheduleMonthly(job, LocalDate.now().dayOfMonth, LocalTime.of(6, 30))
    val dailyRunner = slot<Runnable>()
    verify { executor.scheduleAtFixedRate(capture(dailyRunner), match { it > 0 }, 24 * 60, MINUTES) }
    dailyRunner.captured.run()
    coVerify { job.run() }
  }

  @Test fun `scheduleMonthly not today`() {
    val job = spyk(job)
    runner.scheduleMonthly(job, LocalDate.now().dayOfMonth + 1, LocalTime.of(6, 30))
    val dailyRunner = slot<Runnable>()
    verify { executor.scheduleAtFixedRate(capture(dailyRunner), match { it > 0 }, 24 * 60, MINUTES) }
    dailyRunner.captured.run()
    coVerify(exactly = 0) { job.run() }
  }
}

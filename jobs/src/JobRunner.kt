package klite.jobs

import klite.*
import klite.jdbc.Transaction
import klite.jdbc.TransactionContext
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.DEFAULT
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import java.time.Duration.between
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

fun interface Job {
  suspend fun run()
  val name get() = this::class.simpleName!!
}

class JobRunner(
  private val db: DataSource,
  private val requestIdGenerator: RequestIdGenerator,
  private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(Config.optional("JOB_RUNNER_THREADS", "3").toInt())
): Extension, CoroutineScope {
  override val coroutineContext = executor.asCoroutineDispatcher()
  private val logger = logger()
  private val seq = AtomicLong()
  private val runningJobs = ConcurrentHashMap.newKeySet<kotlinx.coroutines.Job>()

  override fun install(server: Server) {
    server.onStop(::gracefulStop)
  }

  fun runInTransaction(jobName: String, job: Job, start: CoroutineStart = DEFAULT): kotlinx.coroutines.Job {
    val threadName = ThreadNameContext("${requestIdGenerator.prefix}/$jobName#${seq.incrementAndGet()}")
    val tx = Transaction(db)
    return launch(threadName + TransactionContext(tx), start) {
      try {
        logger.info("$jobName started")
        job.run()
        tx.close(true)
      } catch (e: Exception) {
        logger.error("$jobName failed", e)
        tx.close(false)
      }
    }.also { launched ->
      runningJobs += launched
      launched.invokeOnCompletion { runningJobs -= launched }
    }
  }

  fun schedule(job: Job, delay: Long, period: Long, unit: TimeUnit, jobName: String = job.name) {
    val startAt = LocalDateTime.now().plus(delay, unit.toChronoUnit())
    logger.info("$jobName will start at $startAt and run every $period $unit")
    executor.scheduleAtFixedRate({ runInTransaction(jobName, job, UNDISPATCHED) }, delay, period, unit)
  }

  fun scheduleDaily(job: Job, delayMinutes: Long = (Math.random() * 10).toLong(), jobName: String = job.name) =
    schedule(job, delayMinutes, 24 * 60, MINUTES, jobName)

  fun scheduleDaily(job: Job, vararg at: LocalTime, jobName: String = job.name) {
    val now = LocalDateTime.now()
    for (time in at) {
      val todayAt = time.atDate(now.toLocalDate())
      val runAt = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
      scheduleDaily(job, between(now, runAt).toMinutes(), jobName)
    }
  }

  fun scheduleMonthly(job: Job, dayOfMonth: Int, vararg at: LocalTime) {
    scheduleDaily(Job {
      if (LocalDate.now().dayOfMonth == dayOfMonth) job.run()
    }, at = at, job.name)
  }

  private fun gracefulStop() {
    runBlocking {
      runningJobs.forEach { it.cancelAndJoin() }
    }
    executor.shutdown()
    executor.awaitTermination(10, SECONDS)
  }
}

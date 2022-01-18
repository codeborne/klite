package klite.jdbc

import klite.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import java.time.Duration.between
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

class JobRunner(private val db: DataSource): Extension, CoroutineScope {
  private val logger = logger()
  private val executor = Executors.newScheduledThreadPool(Config.optional("JOB_RUNNER_THREADS", "3").toInt())
  override val coroutineContext = executor.asCoroutineDispatcher()
  private val seq = AtomicLong()
  private val runningJobs = ConcurrentHashMap.newKeySet<kotlinx.coroutines.Job>()

  override fun install(server: Server) {
    server.onStop(::gracefulStop)
  }

  fun schedule(job: Job, delay: Long, period: Long, unit: TimeUnit) {
    val jobName = job::class.simpleName
    logger.info("$jobName will start after $delay $unit and run every $period $unit")
    executor.scheduleAtFixedRate({
      val threadName = RequestThreadNameContext("${RequestLogger.prefix}/$jobName#${seq.incrementAndGet()}")
      val tx = Transaction(db)
      val launched = launch(start = UNDISPATCHED, context = TransactionContext(tx) + threadName) {
        try {
          job.run()
          tx.close(true)
        } catch (e: Exception) {
          logger.error("$jobName failed", e)
          tx.close(false)
        }
      }
      runningJobs += launched
      launched.invokeOnCompletion { runningJobs -= launched }
    }, delay, period, unit)
  }

  fun scheduleDaily(job: Job, delayMinutes: Long = (Math.random() * 10).toLong()) =
    schedule(job, delayMinutes, 24 * 60, MINUTES)

  fun scheduleDaily(job: Job, at: LocalTime) {
    val now = LocalDateTime.now()
    val todayAt = at.atDate(now.toLocalDate())
    val runAt = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    scheduleDaily(job, between(now, runAt).toMinutes())
  }

  private fun gracefulStop() {
    runBlocking {
      runningJobs.forEach { it.cancelAndJoin() }
    }
    executor.shutdown()
    executor.awaitTermination(10, SECONDS)
  }
}

fun interface Job {
  suspend fun run()
}

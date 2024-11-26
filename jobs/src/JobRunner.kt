package klite.jobs

import klite.*
import klite.jdbc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.DEFAULT
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import java.time.Duration.between
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource
import kotlin.reflect.full.hasAnnotation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

interface Job {
  suspend fun run()
  val name get() = this::class.simpleName!!
  val allowParallelRun get() = false
}

class NamedJob(override val name: String, override val allowParallelRun: Boolean, private val job: suspend () -> Unit): Job {
  override suspend fun run() = job()
}

open class JobRunner(
  private val db: DataSource,
  private val requestIdGenerator: RequestIdGenerator,
  val workerPool: ScheduledExecutorService = Executors.newScheduledThreadPool(Config.optional("JOB_WORKERS", "3").toInt())
): Extension, CoroutineScope {
  override val coroutineContext = SupervisorJob() + workerPool.asCoroutineDispatcher()
  private val log = logger()
  private val seq = AtomicLong()
  private val runningJobs = ConcurrentHashMap.newKeySet<kotlinx.coroutines.Job>()

  override fun install(server: Server) {
    server.onStop(::gracefulStop)
  }

  internal fun runInTransaction(job: Job, start: CoroutineStart = DEFAULT): kotlinx.coroutines.Job {
    val threadName = ThreadNameContext("${requestIdGenerator.prefix}/${job.name}#${seq.incrementAndGet()}")
    val tx = if (this::class.hasAnnotation<NoTransaction>()) null else Transaction(db)
    return launch(threadName + TransactionContext(tx), start) {
      var commit = true
      try {
        if (!job.allowParallelRun && !db.tryLock(job.name)) return@launch log.info("${job.name} locked, skipping")
        try {
          log.info("${job.name} started")
          run(job)
        } finally {
          if (!job.allowParallelRun) db.unlock(job.name)
        }
      } catch (e: Exception) {
        commit = false
        log.error("${job.name} failed", e)
      } finally {
        tx?.close(commit)
      }
    }.also { launched ->
      runningJobs += launched
      launched.invokeOnCompletion { runningJobs -= launched }
    }
  }

  protected open suspend fun run(job: Job) = job.run()

  open fun runOnce(job: Job) = workerPool.submit { runInTransaction(job, UNDISPATCHED) }

  @Deprecated("Use version with Duration parameters instead", ReplaceWith("schedule(job, period.seconds, delay.seconds)"))
  open fun schedule(job: Job, delay: Long, period: Long, unit: TimeUnit) = schedule(job, unit.toMillis(period).milliseconds, unit.toMillis(delay).milliseconds)

  open fun schedule(job: Job, period: Duration, delay: Duration = period) {
    val startAt = LocalDateTime.now().plus(delay.toJavaDuration()).truncatedTo(ChronoUnit.SECONDS)
    log.info("${job.name} will start at ${startAt.toString().replace("T", " ")} and run every $period")
    workerPool.scheduleAtFixedRate({ runInTransaction(job, UNDISPATCHED) }, delay.inWholeMilliseconds, period.inWholeMilliseconds, MILLISECONDS)
  }

  fun scheduleDaily(job: Job, delay: Duration = (Math.random() * 10).toLong().minutes) =
    schedule(job, 24.hours, delay)

  fun scheduleDaily(job: Job, vararg at: LocalTime) {
    val now = LocalDateTime.now()
    for (time in at) {
      val todayAt = time.atDate(now.toLocalDate())
      val runAt = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
      scheduleDaily(job, between(now, runAt).toKotlinDuration())
    }
  }

  fun scheduleMonthly(job: Job, dayOfMonth: Int, vararg at: LocalTime) = scheduleDaily(NamedJob(job.name, job.allowParallelRun) {
    if (LocalDate.now().dayOfMonth == dayOfMonth) job.run()
  }, at = at)

  open fun gracefulStop() {
    workerPool.shutdown()
    runBlocking {
      runningJobs.forEach { it.cancelAndJoin() }
    }
    workerPool.awaitTermination(10, SECONDS)
  }
}

package klite

import java.time.Instant
import java.util.concurrent.ForkJoinPool

context(Server)
fun Router.metrics(path: String = "/metrics") {
  val base = mapOf(
    "instanceId" to requestIdGenerator.prefix,
    "startedAt" to Instant.now(),
  )

  val workerPool = workerPool as? ForkJoinPool

  get(path) {
    base + mapOf(
      "workerPool" to workerPool?.let {
        mapOf("active" to it.activeThreadCount, "size" to it.poolSize, "max" to it.parallelism)
      }
    )
  }
}

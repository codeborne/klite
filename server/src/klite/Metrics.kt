package klite

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool

object Metrics {
  private val resolvers = ConcurrentHashMap<String, () -> Any?>().apply {
    val startedAt = Instant.now()
    put("startedAt") { startedAt }
  }

  fun register(name: String, value: () -> Any?) {
    resolvers[name] = value
  }

  val data: Map<String, Any?> get() = resolvers.mapValues { it.value() }
}

context(Server)
fun Router.metrics(path: String = "/metrics") {
  (workerPool as? ForkJoinPool)?.let {
    Metrics.register("workerPool") {
      mapOf("active" to it.activeThreadCount, "size" to it.poolSize, "max" to it.parallelism)
    }
  }

  get(path) {
    Metrics.data
  }
}

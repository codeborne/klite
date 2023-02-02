package klite.jdbc

import klite.Config
import klite.warn
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun dockerCompose(vararg args: String) = ProcessBuilder(Config.optional("DOCKER_COMPOSE", "docker-compose"), *args)
  .redirectErrorStream(true).redirectOutput(INHERIT).start().waitFor()

fun startDevDB(service: String = Config.optional("DB_START", "db"), timeout: Duration = 2.seconds) {
  if (service.isEmpty()) return
  try {
    val ms = measureTimeMillis { dockerCompose("up", "-d", service) }
    if (ms > timeout.inWholeMilliseconds / 5) sleep(timeout.inWholeMilliseconds) // give the db more time to start listening
  } catch (e: IOException) {
    System.getLogger("startDevDB").warn("Failed to automatically start $service: $e")
  }
}

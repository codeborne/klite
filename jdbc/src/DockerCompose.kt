package klite.jdbc

import klite.Config
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun dockerCompose(vararg args: String) = ProcessBuilder(Config.optional("DOCKER_COMPOSE", "docker-compose2"), *args)
  .redirectErrorStream(true).redirectOutput(INHERIT).start().waitFor()

fun startDevDB(service: String = Config.optional("DB_START", "db"), timeout: Duration = 2.seconds) {
  if (service.isEmpty()) return
  val ms = measureTimeMillis { dockerCompose("up", "-d", service) }
  if (ms > timeout.inWholeMilliseconds / 5) sleep(timeout.inWholeMilliseconds) // give the db more time to start listening
}

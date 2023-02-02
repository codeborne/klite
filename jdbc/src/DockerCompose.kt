package klite.jdbc

import klite.Config
import klite.info
import klite.warn
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.System.getLogger
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun dockerCompose(command: String): Int = try {
  val fullCommand = Config.optional("DOCKER_COMPOSE", "docker-compose") + " " + command
  getLogger("dockerCompose").info("Starting $fullCommand")
  ProcessBuilder(fullCommand.split(' ')).redirectErrorStream(true).redirectOutput(INHERIT).start().waitFor()
} catch (e: Exception) {
  if (Config.optional("DOCKER_COMPOSE") == null) {
    Config["DOCKER_COMPOSE"] = "docker compose"
    dockerCompose(command)
  } else throw e
}

fun startDevDB(service: String = Config.optional("DB_START", "db"), timeout: Duration = 2.seconds) {
  if (service.isEmpty()) return
  try {
    val ms = measureTimeMillis { dockerCompose("up -d $service") }
    if (ms > timeout.inWholeMilliseconds / 5) sleep(timeout.inWholeMilliseconds) // give the db more time to start listening
  } catch (e: IOException) {
    getLogger("startDevDB").warn("Failed to automatically start $service: $e")
  }
}

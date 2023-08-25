package klite.jdbc

import klite.Config
import klite.info
import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.System.getLogger
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun dockerCompose(command: String): Int = try {
  val fullCommand = Config.optional("DOCKER_COMPOSE", "docker compose") + " " + command
  getLogger("dockerCompose").info(fullCommand)
  getLogger("dockerCompose").info(File(".").absolutePath)
  ProcessBuilder(fullCommand.split(' ')).redirectErrorStream(true).redirectOutput(INHERIT).start().waitFor()
} catch (e: Exception) {
  if (Config.optional("DOCKER_COMPOSE") == null) {
    Config["DOCKER_COMPOSE"] = "docker-compose"
    dockerCompose(command)
  } else throw e
}

fun startDevDB(service: String = Config.optional("DB_START", "db"), timeout: Duration = 2.seconds) {
  if (service.isEmpty()) return
  val timeoutMs = timeout.inWholeMilliseconds
  val ms = measureTimeMillis {
    if (dockerCompose("up -d${if (timeoutMs == 0L) " --wait" else ""} $service") != 0) throw IOException("Failed to start $service")
  }
  if (ms > timeoutMs / 5) sleep(timeoutMs) // give the db more time to start listening
}

package klite.jdbc

import klite.Config
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis

fun dockerCompose(vararg args: String) = ProcessBuilder("docker-compose", *args)
  .redirectErrorStream(true).redirectOutput(INHERIT).start().waitFor()

fun startDevDB(service: String = Config.optional("DB_START", "db")) {
  if (service.isEmpty()) return
  val ms = measureTimeMillis { dockerCompose("up", "-d", service) }
  if (ms > 400) sleep(2000) // give the db more time to start listening
}

package klite

import java.io.File

object Config {
  fun optional(env: String) = System.getProperty(env) ?: System.getenv(env)
  fun optional(env: String, default: String) = optional(env) ?: default
  fun required(env: String) = optional(env) ?: error("$env should be provided as system property or env var")

  fun fromEnvFile(file: File = File(".env")) = file.forEachLine {
    it.split('=', limit = 2).let { System.setProperty(it[0], it[1]) }
  }
}

package klite

import java.io.File

object Config {
  fun optional(env: String) = System.getProperty(env) ?: System.getenv(env)
  fun optional(env: String, default: String) = optional(env) ?: default
  fun required(env: String) = optional(env) ?: error("$env should be provided as system property or env var")

  fun fromEnvFile(file: File = File(".env"), force: Boolean = false) = file.forEachLine {
    it.split('=', limit = 2).let { (key, value) ->
      if (force || optional(key) == null) System.setProperty(key, value)
    }
  }
}

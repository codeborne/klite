package klite

import java.io.File

object Config {
  fun optional(env: String): String? = System.getProperty(env) ?: System.getenv(env)
  fun optional(env: String, default: String) = optional(env) ?: default
  fun required(env: String) = optional(env) ?: error("$env should be provided as system property or env var")

  val active get() = optional("ENV", "dev").split(",")
  fun isActive(conf: String) = active.contains(conf)
  fun isAnyActive(vararg confs: String) = active.any { confs.contains(it) }

  operator fun get(env: String) = required(env)
  operator fun set(env: String, value: String) = System.setProperty(env, value)

  fun useEnvFile(file: File = File(".env"), force: Boolean = false) {
    if (!force && !file.exists()) return logger().info("No ${file.absolutePath} found, skipping")
    file.forEachLine {
      val line = it.trim()
      if (line.isNotEmpty() && !line.startsWith('#'))
        line.split('=', limit = 2).let { (key, value) ->
          if (force || optional(key) == null) set(key, value)
        }
    }
  }
}

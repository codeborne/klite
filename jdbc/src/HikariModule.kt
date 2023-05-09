package klite.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import klite.Config
import klite.info
import klite.logger

class HikariModule(configure: HikariConfig.() -> Unit = {}): DBModule(HikariDataSource(HikariConfig().apply {
  poolName = "app-db"
  jdbcUrl = Config.required("DB_URL")
  username = Config.optional("DB_USER")
  password = Config.optional("DB_PASS")
  minimumIdle = 1
  maximumPoolSize = Config.dbPoolMaxSize
  configure()
  logger().info("Connecting to $jdbcUrl${username?.let { ", user: $username" }}")
}))

package klite.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import klite.*
import klite.StatusCode.Companion.Conflict
import javax.sql.DataSource

class DBModule(urlSuffix: String = "", configure: HikariConfig.() -> Unit = {}): Extension {
  val logger = logger()
  val dataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = Config.required("DB_URL") + urlSuffix
    username = Config.optional("DB_USER")
    password = Config.optional("DB_PASS")
    minimumIdle = 1
    configure()
    logger.info("Connecting to $jdbcUrl")
  })

  override fun install(server: Server) = server.run {
    registry.register<DataSource>(dataSource)
    errors.on(AlreadyExistsException::class, Conflict)
    onStop { dataSource.close() }
  }
}

package klite.liquibase

import klite.Config
import klite.Extension
import klite.Server
import klite.require
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.ResourceAccessor
import org.slf4j.bridge.SLF4JBridgeHandler
import javax.sql.DataSource
import kotlin.concurrent.thread

open class LiquibaseModule(
  val changeSetPath: String = Config.optional("LIQUIBASE_CHANGESET", "db.xml"),
  val resourceAccessor: ResourceAccessor = ClassLoaderResourceAccessor()
): Extension {
  override fun install(server: Server) = server.run {
    redirectJavaLogging()
    migrate(require(), Config.optional("ENV", "dev").split(","))
  }

  fun migrate(db: DataSource, configs: List<String>) {
    db.connection.use { conn ->
      val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(conn))
      val liquibase = Liquibase(changeSetPath, resourceAccessor, database)
      val shutdownHook = thread(start = false) { liquibase.forceReleaseLocks() }
      Runtime.getRuntime().addShutdownHook(shutdownHook)
      update(liquibase, configs)
      Runtime.getRuntime().removeShutdownHook(shutdownHook)
    }
  }

  open fun update(liquibase: Liquibase, configs: List<String>) {
    if (configs.contains("test")) liquibase.dropAll()
    liquibase.update(configs.joinToString(","))
  }

  open fun redirectJavaLogging() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
  }
}

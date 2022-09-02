package klite.liquibase

import klite.*
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.ResourceAccessor
import org.slf4j.bridge.SLF4JBridgeHandler
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource
import kotlin.concurrent.thread

open class LiquibaseModule(
  val changeSetPath: String = Config.optional("LIQUIBASE_CHANGESET", "db.xml"),
  val resourceAccessor: ResourceAccessor = ClassLoaderResourceAccessor(),
  private val dropAllBeforeUpdate: Boolean = false,
  private val dropAllOnUpdateFailure: Boolean = Config.isTest
): Extension {
  companion object {
    init {
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
      Config.overridable("LOGGER.liquibase.servicelocator", "ERROR")
    }
  }

  override fun install(server: Server) {
    migrate(Config.active, server.optional<DataSource>()?.connection)
  }

  fun exec(connection: Connection? = null, block: Liquibase.() -> Unit) {
    (connection ?: DriverManager.getConnection(Config["DB_URL"], Config.optional("DB_USER"), Config.optional("DB_PASS"))).use { conn ->
      val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(conn))
      val liquibase = Liquibase(changeSetPath, resourceAccessor, database)
      val shutdownHook = thread(start = false) { liquibase.forceReleaseLocks() }
      Runtime.getRuntime().addShutdownHook(shutdownHook)
      liquibase.block()
      Runtime.getRuntime().removeShutdownHook(shutdownHook)
    }
  }

  fun migrate(contexts: List<String>, connection: Connection? = null) = exec(connection) {
    if (dropAllBeforeUpdate) dropAll()
    update(this, Contexts(contexts))
  }

  open fun update(liquibase: Liquibase, contexts: Contexts) {
    try {
      liquibase.update(contexts)
    } catch (e: LiquibaseException) {
      if (dropAllOnUpdateFailure) {
        logger().warn("DB Updated failed, dropping all to retry")
        liquibase.dropAll()
        liquibase.update(contexts)
      }
      else throw e
    }
  }
}

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
import javax.sql.DataSource
import kotlin.concurrent.thread

open class LiquibaseModule(
  val changeSetPath: String = Config.optional("LIQUIBASE_CHANGESET", "db.xml"),
  val resourceAccessor: ResourceAccessor = ClassLoaderResourceAccessor(),
  private val dropAllOnUpdateFailureInConfigs: Collection<String> = setOf("test")
): Extension {
  override fun install(server: Server) = server.run {
    redirectJavaLogging()
    migrate(require(), Config.active)
  }

  fun migrate(db: DataSource, contexts: List<String>) {
    db.connection.use { conn ->
      val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(conn))
      val liquibase = Liquibase(changeSetPath, resourceAccessor, database)
      val shutdownHook = thread(start = false) { liquibase.forceReleaseLocks() }
      Runtime.getRuntime().addShutdownHook(shutdownHook)
      update(liquibase, Contexts(contexts))
      Runtime.getRuntime().removeShutdownHook(shutdownHook)
    }
  }

  open fun update(liquibase: Liquibase, contexts: Contexts) {
    try {
      liquibase.update(contexts)
    } catch (e: LiquibaseException) {
      if (dropAllOnUpdateFailureInConfigs.any { contexts.contexts.contains(it) }) {
        logger().warn("DB Updated failed, dropping all to retry")
        liquibase.dropAll()
        liquibase.update(contexts)
      }
      else throw e
    }
  }

  open fun redirectJavaLogging() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
  }
}

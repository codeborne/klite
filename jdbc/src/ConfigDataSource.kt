package klite.jdbc

import klite.Config
import klite.info
import klite.logger
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLFeatureNotSupportedException
import java.util.*
import javax.sql.DataSource

/** Non-pooled basic datasource, see also [PooledDataSource] */
open class ConfigDataSource(
  val url: String = Config["DB_URL"],
  val user: String? = Config.optional("DB_USER"),
  pass: String? = Config.optional("DB_PASS"),
  val isReadOnly: Boolean = Config.optional("DB_READONLY") == "true",
  val props: Properties = Properties()
): DataSource {
  init {
    user?.let { props["user"] = it }
    pass?.let { props["password"] = it }
    if (url.startsWith("jdbc:postgresql") && "autosave" !in props) {
      // handle "cached plan must not change result type" Postgres error if schema changes
      // https://stackoverflow.com/questions/2783813/postgres-error-cached-plan-must-not-change-result-type
      props["autosave"] = "conservative"
    }
    if (isReadOnly) {
      props["readOnly"] = "true"
      props["readOnlyMode"] = "always"
    }
  }

  init {
    logger().info("Connecting to $url${user?.let { ", user: $user" } ?: ""}")
  }

  override fun getConnection(): Connection = DriverManager.getConnection(url, props)
  override fun getConnection(user: String?, pass: String?) = throw SQLFeatureNotSupportedException("Use getConnection()")

  override fun getLogWriter(): PrintWriter? = null
  override fun setLogWriter(out: PrintWriter?) = throw SQLFeatureNotSupportedException()

  override fun getLoginTimeout() = 0
  override fun setLoginTimeout(seconds: Int) = throw SQLFeatureNotSupportedException()

  @Suppress("UNCHECKED_CAST")
  override fun <T> unwrap(iface: Class<T>): T = iface.cast(this)
  override fun isWrapperFor(iface: Class<*>) = iface.isAssignableFrom(javaClass)

  override fun getParentLogger() = throw SQLFeatureNotSupportedException()
  override fun createConnectionBuilder() = throw SQLFeatureNotSupportedException()
}


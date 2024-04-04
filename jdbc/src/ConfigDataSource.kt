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
  val isReadOnly: Boolean = Config.optional("DB_READONLY") == "true"
): DataSource {
  private val props = Properties().apply {
    user?.let { put("user", it) }
    pass?.let { put("password", it) }
    if (isReadOnly) {
      put("readOnly", "true")
      put("readOnlyMode", "always")
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

  override fun <T: Any?> unwrap(iface: Class<T>?) = null
  override fun isWrapperFor(iface: Class<*>?) = false

  override fun getParentLogger() = throw SQLFeatureNotSupportedException()
  override fun createConnectionBuilder() = throw SQLFeatureNotSupportedException()
}

package klite.jdbc

import klite.Config
import klite.info
import klite.logger
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLFeatureNotSupportedException
import javax.sql.DataSource

/** Non-pooled basic datasource, see also [PooledDataSource] */
open class ConfigDataSource(
  val url: String = Config["DB_URL"],
  val user: String? = Config.optional("DB_USER"),
  private val pass: String? = Config.optional("DB_PASS"),
): DataSource {
  init {
    logger().info("Connecting to $url${user?.let { ", user: $user" }}")
  }

  override fun getConnection() = getConnection(user, pass)
  override fun getConnection(user: String?, pass: String?): Connection = DriverManager.getConnection(url, user, pass)

  override fun getLogWriter(): PrintWriter? = null
  override fun setLogWriter(out: PrintWriter?) = throw SQLFeatureNotSupportedException()

  override fun getLoginTimeout() = 0
  override fun setLoginTimeout(seconds: Int) = throw SQLFeatureNotSupportedException()

  override fun <T: Any?> unwrap(iface: Class<T>?) = null
  override fun isWrapperFor(iface: Class<*>?) = false

  override fun getParentLogger() = throw SQLFeatureNotSupportedException()
  override fun createConnectionBuilder() = throw SQLFeatureNotSupportedException()
}

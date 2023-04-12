package klite.jdbc

import java.sql.ResultSet
import javax.sql.DataSource

fun DataSource.lock(on: String) = query("select pg_advisory_lock(${on.hashCode()})") {}.first()
fun DataSource.tryLock(on: String): Boolean = query("select pg_try_advisory_lock(${on.hashCode()})") { getBoolean(1) }.first()
fun DataSource.unlock(on: String): Boolean = query("select pg_advisory_unlock(${on.hashCode()})") { getBoolean(1) }.first()

private val columnNameIndexMapField = runCatching {
  Class.forName("org.postgresql.jdbc.PgResultSet").getDeclaredField("columnNameIndexMap").apply { trySetAccessible() }
}.getOrNull()

/**
 * Pre-populates PgResultSet.columnNameIndexMap with duplicate (joined columns) prefixed with their index,
 * making it possible to use getString("2.id") to get second "id" column, etc.
 */
internal fun ResultSet.populatePgColumnNameIndex(select: String) {
  if (columnNameIndexMapField == null || !select.contains("join", ignoreCase = true)) return
  val rs = unwrap(columnNameIndexMapField.declaringClass) ?: return
  val md = metaData
  val map = HashMap<String, Int>()
  var joinCount = 0
  for (i in 1..md.columnCount) {
    val label = md.getColumnLabel(i)
    if (label == "id") joinCount++
    map.putIfAbsent(label, i)
    if (joinCount > 1) map.putIfAbsent("$joinCount.$label", i)
  }
  columnNameIndexMapField.set(rs, map)
}

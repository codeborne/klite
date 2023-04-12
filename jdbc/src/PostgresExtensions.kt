package klite.jdbc

import klite.trimToNull
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.text.RegexOption.IGNORE_CASE
import kotlin.text.RegexOption.MULTILINE

fun DataSource.lock(on: String) = query("select pg_advisory_lock(${on.hashCode()})") {}.first()
fun DataSource.tryLock(on: String): Boolean = query("select pg_try_advisory_lock(${on.hashCode()})") { getBoolean(1) }.first()
fun DataSource.unlock(on: String): Boolean = query("select pg_advisory_unlock(${on.hashCode()})") { getBoolean(1) }.first()

private val columnNameIndexMapField = runCatching {
  Class.forName("org.postgresql.jdbc.PgResultSet").getDeclaredField("columnNameIndexMap").apply { trySetAccessible() }
}.getOrNull()

/**
 * Pre-populates PgResultSet.columnNameIndexMap with duplicate (joined columns) prefixed with their index,
 * making it possible to use getString("alias.id") to get other table's "id" column, etc.
 */
internal fun ResultSet.populatePgColumnNameIndex(select: String) {
  if (columnNameIndexMapField == null || !select.contains("join", ignoreCase = true)) return
  val rs = unwrap(columnNameIndexMapField.declaringClass) ?: return
  val md = metaData
  val map = HashMap<String, Int>()
  val joinAliases = joinAliases(select)
  var joinCount = 0
  for (i in 1..md.columnCount) {
    val label = md.getColumnLabel(i)
    if (label == "id") joinCount++
    map.putIfAbsent(label, i)
    if (joinCount > 1) map.putIfAbsent("${joinAliases.getOrNull(joinCount - 2) ?: joinCount}.$label", i)
  }
  columnNameIndexMapField.set(rs, map)
}

val joinRegex = "\\bjoin\\s+(\\w+?)(\\s+as)?(\\s+(\\w+?))?\\s+(on|using)\\b".toRegex(setOf(IGNORE_CASE, MULTILINE))
internal fun joinAliases(select: String) = joinRegex.findAll(select).map { it.groupValues[4].trimToNull() ?: it.groupValues[1] }.toList()

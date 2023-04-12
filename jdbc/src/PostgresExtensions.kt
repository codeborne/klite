package klite.jdbc

import java.sql.ResultSet

private val columnNameIndexMapField = runCatching {
  Class.forName("org.postgresql.jdbc.PgResultSet").getDeclaredField("columnNameIndexMap").apply { trySetAccessible() }
}.getOrNull()

/**
 * Pre-populates PgResultSet.columnNameIndexMap with duplicate (joined columns) prefixed with their index,
 * making it possible to use getString("2.id") to get second "id" column, etc.
 */
internal fun ResultSet.populatePgColumnNameIndex(select: String) {
  try {
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
  } catch (ignore: Exception) {}
}

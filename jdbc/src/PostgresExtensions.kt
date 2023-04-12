package klite.jdbc

import java.sql.ResultSet

/**
 * Pre-populates PgResultSet.columnNameIndexMap with duplicate (joined columns) prefixed with their index,
 * making it possible to use getString("2.id") to get second "id" column, etc.
 */
internal fun ResultSet.populatePgColumnNameIndex() = try {
  val field = javaClass.getField("columnNameIndexMap")
  val md = metaData
  val map = mutableMapOf<String, Int>()
  var joinCount = 1
  for (i in 1..md.columnCount) {
    val label = md.getColumnLabel(i)
    if (label == "id") joinCount++
    map.putIfAbsent(label, i)
    map.putIfAbsent("$joinCount.$label", i)
  }
  field.set(this, map)
} catch (ignore: Exception) {}

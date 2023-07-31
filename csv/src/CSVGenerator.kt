package klite.csv

import java.io.OutputStream
import java.sql.ResultSet

const val bomUTF8 = "\uFEFF"
private val needsQuotes = "[\\s\"';,]".toRegex()

open class CSVGenerator(val out: OutputStream, val separator: String = ",", bom: String = bomUTF8) {
  init { out.write(bom.toByteArray()) }

  fun row(vararg values: Any?) = this.apply {
    out.write(values.joinToString(separator, postfix = "\n", transform = ::transform).toByteArray())
  }

  protected open fun transform(o: Any?): String = when(o) {
    null -> ""
    is Number -> if (separator == ";") o.toString().replace(".", ",") else o.toString()
    is String -> if (o.contains(needsQuotes)) "\"${o.replace("\"", "\"\"")}\"" else o
    else -> transform(o.toString())
  }

  private fun sqlHeader(rs: ResultSet) = row(*(1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }.toTypedArray())
  fun sqlDump(rs: ResultSet) {
    if (rs.isFirst) sqlHeader(rs)
    row(*(1..rs.metaData.columnCount).map { rs.getObject(it) }.toTypedArray())
  }
}

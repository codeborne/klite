package klite.csv

import java.io.OutputStream
import java.sql.ResultSet
import kotlin.text.Charsets.UTF_8

open class CSVGenerator(val out: OutputStream, val separator: String = ",", bom: ByteArray = "\uFEFF".toByteArray()) {
  init { out.write(bom) }

  fun row(vararg values: Any?) = this.apply {
    out.write(values.joinToString(separator, postfix = "\n", transform = ::transform).toByteArray(UTF_8))
  }

  protected open fun transform(o: Any?): String = when(o) {
    is Number -> if (separator == ";") o.toString().replace(".", ",") else o.toString()
    is String -> if (o.contains("[\\s\"';,]".toRegex())) "\"${o.replace("\"", "\"\"")}\"" else o
    else -> transform(o?.toString()) ?: ""
  }

  private fun sqlHeader(rs: ResultSet) = row(*(1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }.toTypedArray())
  fun sqlDump(rs: ResultSet) {
    if (rs.isFirst) sqlHeader(rs)
    row(*(1..rs.metaData.columnCount).map { rs.getObject(it) }.toTypedArray())
  }
}

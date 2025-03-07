package klite.csv

import java.io.OutputStream
import java.nio.charset.Charset
import java.sql.ResultSet
import kotlin.text.Charsets.UTF_8

const val bomUTF8 = "\uFEFF"
private val needsQuotes = "[\\s\"';,]".toRegex()

open class CSVGenerator(val out: OutputStream, val separator: String = ",", val charset: Charset = UTF_8, bom: String = if (charset == UTF_8) bomUTF8 else "") {
  init { out.write(bom.toByteArray(charset)) }

  fun row(vararg values: Any?) = this.apply {
    out.write(values.joinToString(separator, postfix = "\n", transform = ::transform).toByteArray(charset))
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

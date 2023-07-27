package klite.csv

import java.io.InputStream

class CSVParser(separator: String = ",") {
  private val splitter = """(?:$separator|^)("((?:(?:"")*[^"]*)*)"|([^"$separator]*))""".toRegex()

  fun parse(stream: InputStream): Sequence<Map<String, String>> {
    val lines = stream.bufferedReader().lineSequence().iterator()
    var headerLine = lines.next()
    if (headerLine.startsWith(bomUTF8)) headerLine = headerLine.substring(bomUTF8.length)
    val header = splitLine(headerLine).toList()
    return lines.asSequence().map {
      splitLine(it).withIndex().associate { header[it.index] to it.value }
    }
  }

  internal fun splitLine(line: String) = splitter.findAll(line).map {
    val values = it.groupValues.drop(2)
    values[0].ifEmpty { values[1] }.replace("\"\"", "\"")
  }
}

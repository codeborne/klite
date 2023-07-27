package klite.csv

import java.io.InputStream

class CSVParser(separator: String = ",", private val skipBOM: Boolean = true) {
  private val splitter = """(?:$separator|^)("((?:(?:"")*[^"]*)*)"|([^"$separator]*))""".toRegex()

  fun parse(stream: InputStream): Sequence<Map<String, String>> {
    if (skipBOM) stream.read(ByteArray(3))
    val lines = stream.bufferedReader().lineSequence().iterator()
    val header = splitLine(lines.next()).toList()
    return lines.asSequence().map {
      splitLine(it).withIndex().associate { header[it.index] to it.value }
    }
  }

  internal fun splitLine(line: String) = splitter.findAll(line).map {
    val values = it.groupValues.drop(2)
    values[0].ifEmpty { values[1] }.replace("\"\"", "\"")
  }
}

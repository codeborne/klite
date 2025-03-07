package klite.csv

import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

class CSVParser(separator: String = ",", private val charset: Charset = UTF_8) {
  private val splitter = """(?:$separator|^)("((?:(?:"")*[^"]*)*)"|([^"$separator]*))""".toRegex()

  fun parse(stream: InputStream): Sequence<Map<String, String>> {
    val lines = stream.bufferedReader(skipBOM(stream)).lineSequence().iterator()
    var headerLine = lines.next()
    if (headerLine.startsWith(bomUTF8)) headerLine = headerLine.substring(bomUTF8.length)
    val header = splitLine(headerLine).toList()
    return lines.asSequence().map {
      splitLine(it).withIndex().associate { header[it.index] to it.value }
    }
  }

  private fun skipBOM(stream: InputStream): Charset {
    if (stream.markSupported()) {
      stream.mark(2)
      stream.readNBytes(2).let {
        if (it.contentEquals(bomUTF8.toByteArray())) return UTF_8
        else stream.reset()
      }
    }
    return charset
  }

  internal fun splitLine(line: String) = splitter.findAll(line).map {
    val values = it.groupValues.drop(2)
    values[0].ifEmpty { values[1] }.replace("\"\"", "\"")
  }
}

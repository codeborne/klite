package klite.json

import org.intellij.lang.annotations.Language
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.text.ParseException

class JsonParser {
  fun parse(json: Reader): Any? = JsonReader(json).readValue()
  fun parse(@Language("JSON") json: String) = parse(json.reader())
  fun parse(json: InputStream) = parse(BufferedReader(InputStreamReader(json)))

  // inline fun <reified T: Any> parse(json: String) = parse(Scanner(json), T::class)
}

private class JsonReader(val reader: Reader) {
  private var pos: Int = 0

  fun readValue(): Any? {
    val next = nextNonWhitespace()
    return if (next == '"') readUntil('"') // TODO escaped \" or \\u stuff
    else if (next == '{') readObject()
    else if (next == '[') readArray()
    else if (next.isDigit()) readFrom(next) { it.isDigit() || it == '.' }.let { it.toIntOrNull() ?: it.toLongOrNull() ?: it.toDouble() }
    else if (next == 't' || next == 'f') readFrom(next) { it.isLetter() }.toBoolean()
    else if (next == 'n') readFrom(next) { it.isLetter() }.let { if (it == "null") null else fail("Unexpected $it") }
    else fail("Unexpected char: $next")
  }

  private fun readObject(): Map<String, Any?> {
    val o = mutableMapOf<String, Any?>()
    while (true) {
      var next = nextNonWhitespace()
      if (next == '}') return o else next.shouldBe('"')

      val key = readUntil('"')
      nextNonWhitespace().shouldBe(':')
      o[key] = readValue()

      next = nextNonWhitespace()
      if (next == '}') return o else next.shouldBe(',')
    }
  }

  private fun readArray(): List<Any?> {
    val a = mutableListOf<Any?>()
    while (true) {
      reader.mark(100)
      var next = nextNonWhitespace()
      if (next == ']') return a else reader.reset()
      a += readValue()
      next = nextNonWhitespace()
      if (next == ']') return a else next.shouldBe(',')
    }
  }

  private fun nextNonWhitespace(): Char {
    var char: Char
    do { char = read().toChar() } while (char.isWhitespace())
    return char
  }

  private fun Char.shouldBe(char: Char) {
    if (this != char) fail("Expecting $char, but got $this")
  }

  private fun fail(msg: String): Nothing = throw ParseException(msg, pos)

  private fun readUntil(until: Char) = readFrom { it != until }

  private fun readFrom(include: Char? = null, cont: (Char) -> Boolean): String {
    val into = StringBuilder()
    val reset = include != null
    if (reset) into.append(include)
    while (true) {
      if (reset) reader.mark(1)
      val char = read()
      if (char < 0 || !cont(char.toChar())) return into.toString().also { if (reset) reader.reset() }
      else into.append(char.toChar())
    }
  }

  private fun read(): Int = reader.read().also { pos++ }
}

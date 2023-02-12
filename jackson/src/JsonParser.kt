package klite.json

import org.intellij.lang.annotations.Language
import java.io.InputStream
import java.io.Reader
import java.text.ParseException

class JsonParser {
  fun parse(json: Reader): Any? = JsonReader(json).readValue()
  fun parse(@Language("JSON") json: String) = parse(json.reader())
  fun parse(json: InputStream) = parse(json.reader())

  // inline fun <reified T: Any> parse(json: String) = parse(Scanner(json), T::class)
}

private const val EOF = '\uFFFF'

private class JsonReader(private val reader: Reader) {
  private var pos: Int = 0
  private var nextChar: Char? = null

  fun readValue(): Any? {
    val next = nextNonSpace()
    return if (next == '"') readString()
    else if (next == '{') readObject()
    else if (next == '[') readArray()
    else if (next.isDigit() || next == '-' || next == '+') readWhile(next) { it.isDigit() || it == '.' }.let { it.toIntOrNull() ?: it.toLongOrNull() ?: it.toDouble() }
    else if (next == 't' || next == 'f') readWhile(next) { it.isLetter() }.toBoolean()
    else if (next == 'n') readWhile(next) { it.isLetter() }.let { if (it == "null") null else fail("Unexpected $it") }
    else fail("Unexpected char: $next")
  }

  private fun readString(): String {
    val sb = StringBuilder()
    while (true) {
      when (val c = read()) {
        '\\' -> sb.append(readEscapedChar())
        '"' -> return sb.toString()
        else -> sb.append(c)
      }
    }
  }

  private fun readEscapedChar() = when (val c = read()) {
    'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; 'b' -> '\b'; 'f' -> '\u000C'
    'u' -> (1..4).map { read() }.joinToString("").toInt(16).toChar()
    else -> c
  }

  private fun readObject(): Map<String, Any?> {
    val o = mutableMapOf<String, Any?>()
    while (true) {
      var next = nextNonSpace()
      if (next == '}') return o else next.expect('"')

      val key = readString()
      nextNonSpace().expect(':')
      o[key] = readValue()

      next = nextNonSpace()
      if (next == '}') return o else next.expect(',')
    }
  }

  private fun readArray(): List<Any?> {
    val a = mutableListOf<Any?>()
    while (true) {
      var char = nextNonSpace()
      if (char == ']') return a else nextChar = char
      a += readValue()
      char = nextNonSpace()
      if (char == ']') return a else char.expect(',')
    }
  }

  private fun nextNonSpace(): Char {
    var char: Char
    do { char = read() } while (char.isWhitespace())
    return char
  }

  private fun readWhile(include: Char? = null, cont: (Char) -> Boolean): String {
    val into = StringBuilder()
    if (include != null) into.append(include)
    while (true) {
      val char = read()
      if (char == EOF || !cont(char)) return into.toString().also { if (include != null) nextChar = char }
      else into.append(char)
    }
  }

  private fun read(): Char = nextChar?.also { nextChar = null } ?: reader.read().toChar().also { pos++ }

  private fun fail(msg: String): Nothing = throw JsonParseException(msg, pos - 1)

  private fun Char.expect(char: Char) {
    if (this != char) fail("Expecting $char but got ${if (this == EOF) "EOF" else this}")
  }
}

class JsonParseException(msg: String, pos: Int): ParseException("$msg at index $pos", pos)

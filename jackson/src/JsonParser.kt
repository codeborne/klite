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

private class JsonReader(private val reader: Reader) {
  private var pos: Int = 0
  private var nextChar: Char? = null

  fun readValue(): Any? {
    val next = readNonWhitespace()
    return if (next == '"') readUntil('"') // TODO escaped \" or \\u stuff
    else if (next == '{') readObject()
    else if (next == '[') readArray()
    else if (next.isDigit()) readWhile(next) { it.isDigit() || it == '.' }.let { it.toIntOrNull() ?: it.toLongOrNull() ?: it.toDouble() }
    else if (next == 't' || next == 'f') readWhile(next) { it.isLetter() }.toBoolean()
    else if (next == 'n') readWhile(next) { it.isLetter() }.let { if (it == "null") null else fail("Unexpected $it") }
    else fail("Unexpected char: $next")
  }

  private fun readObject(): Map<String, Any?> {
    val o = mutableMapOf<String, Any?>()
    while (true) {
      var next = readNonWhitespace()
      if (next == '}') return o else next.shouldBe('"')

      val key = readUntil('"')
      readNonWhitespace().shouldBe(':')
      o[key] = readValue()

      next = readNonWhitespace()
      if (next == '}') return o else next.shouldBe(',')
    }
  }

  private fun readArray(): List<Any?> {
    val a = mutableListOf<Any?>()
    while (true) {
      var char = readNonWhitespace()
      if (char == ']') return a else nextChar = char
      a += readValue()
      char = readNonWhitespace()
      if (char == ']') return a else char.shouldBe(',')
    }
  }

  private fun readNonWhitespace(): Char {
    var char: Char
    do { char = read().toChar() } while (char.isWhitespace())
    return char
  }

  private fun readUntil(until: Char) = readWhile { it != until }

  private fun readWhile(include: Char? = null, cont: (Char) -> Boolean): String {
    val into = StringBuilder()
    nextChar = include
    while (true) {
      val char = read()
      if (char < 0 || !cont(char.toChar())) return into.toString().also { if (include != null) nextChar = char.toChar() }
      else into.append(char.toChar())
    }
  }

  private fun read(): Int = nextChar?.code?.also { nextChar = null } ?: reader.read().also { pos++ }

  private fun fail(msg: String): Nothing = throw JsonParseException(msg, pos - 1)

  private fun Char.shouldBe(char: Char) {
    if (this != char) fail("Expecting $char but got $this")
  }
}

class JsonParseException(msg: String, pos: Int): ParseException("$msg at index $pos", pos)

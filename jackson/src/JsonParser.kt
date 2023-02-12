package klite.json

import klite.Converter
import klite.fromValues
import org.intellij.lang.annotations.Language
import java.io.InputStream
import java.io.Reader
import java.text.ParseException
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class JsonParser {
  fun parse(json: Reader, type: KClass<*>? = null): Any? = JsonReader(json).readValue(type)
  fun parse(@Language("JSON") json: String, type: KClass<*>? = null) = parse(json.reader(), type)
  fun parse(json: InputStream, type: KClass<*>? = null) = parse(json.reader(), type)
}

private const val EOF = '\uFFFF'

private class JsonReader(private val reader: Reader) {
  private var pos: Int = 0
  private var nextChar: Char? = null

  fun readValue(type: KClass<*>?): Any? = when (val c = nextNonSpace()) {
    '"' -> type.from(readString())
    '{' -> readObject(type)
    '[' -> readArray()
    '-', '+', in '0'..'9' -> readNumber(c, type)
    't', 'f' -> readLettersOrDigits(c).toBoolean()
    'n' -> readLettersOrDigits(c).let { if (it == "null") null else fail("Unexpected $it") }
    else -> fail("Unexpected char: $c")
  }

  private fun readString(): String = StringBuilder().apply {
    while (true) {
      when (val c = read()) {
        '"' -> break
        '\\' -> append(readEscapedChar())
        EOF -> fail("Unfinished string, EOF")
        else -> append(c)
      }
    }
  }.toString()

  private fun readEscapedChar() = when (val c = read()) {
    'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; 'b' -> '\b'; 'f' -> '\u000C'
    'u' -> (1..4).map { read() }.joinToString("").toInt(16).toChar()
    else -> c
  }

  private fun readNumber(c: Char, type: KClass<*>?) = readLettersOrDigits(c).let { s ->
    type?.from(s) ?: s.toIntOrNull() ?: s.toLongOrNull() ?: s.toDouble()
  }

  private fun readObject(type: KClass<*>?) = mutableMapOf<String, Any?>().apply {
    while (true) {
      var next = nextNonSpace()
      if (next == '}') break else next.expect('"')

      val key = readString()
      nextNonSpace().expect(':')
      this[key] = readValue(type?.primaryConstructor?.parameters?.find { it.name == key }?.type?.classifier as? KClass<*>)

      next = nextNonSpace()
      if (next == '}') break else next.expect(',')
    }
  }.let { if (type != null) it.fromValues(type) else it }

  private fun readArray() = mutableListOf<Any?>().apply {
    while (true) {
      var c = nextNonSpace()
      if (c == ']') break else nextChar = c
      add(readValue(null)) // TODO
      c = nextNonSpace()
      if (c == ']') break else c.expect(',')
    }
  }

  private fun nextNonSpace(): Char {
    var char: Char
    do { char = read() } while (char.isWhitespace())
    return char
  }

  private fun readLettersOrDigits(include: Char? = null): String = StringBuilder().apply {
    if (include != null) append(include)
    while (true) {
      val c = read()
      if (c == EOF || !(c.isLetterOrDigit() || c == '.')) { if (include != null) { nextChar = c }; break }
      else append(c)
    }
  }.toString()

  private fun read(): Char = nextChar?.also { nextChar = null } ?: reader.read().toChar().also { pos++ }

  private fun fail(msg: String): Nothing = throw JsonParseException(msg, pos - 1)

  private fun Char.expect(char: Char) {
    if (this != char) fail("Expecting $char but got ${if (this == EOF) "EOF" else this}")
  }

  private fun KClass<*>?.from(s: String) = if (this != null) Converter.from(s, this) else s
}

class JsonParseException(msg: String, pos: Int): ParseException("$msg at index $pos", pos)

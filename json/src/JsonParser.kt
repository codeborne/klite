package klite.json

import klite.Converter
import klite.createFrom
import klite.publicProperties
import klite.trimToNull
import java.io.Reader
import java.text.ParseException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

private const val EOF = '\uFFFF'

internal class JsonParser(private val reader: Reader, private val opts: JsonMapper) {
  private var pos: Int = 0
  private var nextChar: Char? = null

  fun readValue(type: KType?): Any? = opts.values.from(when (val c = nextNonSpace()) {
    '"' -> type.from(readString().let { if (opts.trimToNull) it.trimToNull() else it })
    '{' -> readObject(type?.takeIfSpecific())
    '[' -> readArray(type)
    '-', '+', in '0'..'9' -> readNumber(c, type)
    't', 'f' -> readLettersOrDigits(c).toBoolean()
    'n' -> readLettersOrDigits(c).let { if (it == "null") null else fail("Unexpected $it") }
    else -> fail("Unexpected char: $c")
  })

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

  private fun readNumber(c: Char, type: KType?) = readLettersOrDigits(c).let { s ->
    type?.takeIfSpecific()?.from(s) ?: s.toIntOrNull() ?: s.toLongOrNull() ?: s.toDouble()
  }

  private fun readObject(type: KType?) = mutableMapOf<String, Any?>().let {
    val props = (type?.classifier as? KClass<*>)?.publicProperties?.associateBy { prop ->
      prop.findAnnotation<JsonProperty>()?.value?.trimToNull() ?: prop.name
    }
    while (true) {
      var next = nextNonSpace()
      if (next == '}') break else next.expect('"')

      val key = opts.keys.from(readString())
      nextNonSpace().expect(':')
      val prop = props?.get(key)
      val v = readValue(prop?.returnType)
      if (prop == null || prop.findAnnotation<JsonProperty>()?.readOnly != true && !prop.hasAnnotation<JsonIgnore>())
        it[prop?.name ?: key] = v

      next = nextNonSpace()
      if (next == '}') break else next.expect(',')
    }
    type?.createFrom(it) ?: it
  }

  private fun readArray(type: KType?) = collectionOf(type).apply {
    while (true) {
      var c = nextNonSpace()
      if (c == ']') break else nextChar = c
      add(readValue(type?.arguments?.first()?.type))
      c = nextNonSpace()
      if (c == ']') break else c.expect(',')
    }
  }

  private fun collectionOf(type: KType?): MutableCollection<Any?> = when (type?.classifier) {
    Set::class -> mutableSetOf()
    else -> mutableListOf()
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

  // TODO: puzzler: remove <Any>
  private fun KType?.from(s: String?) = if (this != null && s != null) Converter.from<Any>(s, this) else s
  private fun KType.takeIfSpecific() = takeIf { classifier != Any::class && classifier != Map::class }
}

class JsonParseException(msg: String, pos: Int): ParseException("$msg at index $pos", pos)

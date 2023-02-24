package klite.json

import org.intellij.lang.annotations.Language
import java.io.*
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Target(PROPERTY) annotation class JsonIgnore
@Target(PROPERTY) annotation class JsonProperty(val value: String = "", val readOnly: Boolean = false)

data class JsonMapper(
  val trimToNull: Boolean = true,
  val renderNulls: Boolean = false,
  val keys: NameConverter = NameConverter(),
  val values: ValueConverter<Any?> = ValueConverter()
) {
  fun <T> parse(json: Reader, type: KType?): T = JsonParser(json, this).readValue(type) as T
  fun <T> parse(@Language("JSON") json: String, type: KType?): T = parse(json.reader(), type) as T
  fun <T> parse(json: InputStream, type: KType?): T = parse(json.reader(), type) as T

  fun render(o: Any?, out: Writer) = JsonRenderer(out, this).render(o)
  fun render(o: Any?, out: OutputStream) = OutputStreamWriter(out).let { render(o, it); it.flush() }
  @Language("JSON") fun render(o: Any?): String = FastStringWriter().also { render(o, it) }.toString()
}

inline fun <reified T> JsonMapper.parse(json: Reader): T = parse(json, typeOf<T>())
inline fun <reified T> JsonMapper.parse(@Language("JSON") json: String): T = parse(json, typeOf<T>())
inline fun <reified T> JsonMapper.parse(json: InputStream): T = parse(json, typeOf<T>())

typealias NameConverter = ValueConverter<String>
open class ValueConverter<T> {
  open fun to(o: T) = o
  open fun from(o: T) = o
}

object SnakeCase: NameConverter() {
  private val humps = "(?<=.)(?=\\p{Upper})".toRegex()
  override fun to(o: String) = o.replace(humps, "_").lowercase()
  override fun from(o: String) = o.split('_').joinToString("") { it.replaceFirstChar { it.uppercaseChar() } }.replaceFirstChar { it.lowercaseChar() }
}

class FastStringWriter: Writer() {
  private val buf = StringBuilder()
  override fun close() {}
  override fun flush() {}
  override fun toString() = buf.toString()

  override fun write(c: Int) { buf.append(c.toChar()) }
  override fun write(s: String) { buf.append(s) }
  override fun write(cbuf: CharArray, off: Int, len: Int) { buf.append(cbuf, off, len) }
}

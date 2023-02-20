package klite.json

import org.intellij.lang.annotations.Language
import java.io.*
import kotlin.reflect.KType

// TODO: support these
annotation class JsonIgnore
annotation class JsonProperty(val value: String = "", val readOnly: Boolean = false)

class JsonMapper(val opts: JsonOptions = JsonOptions()) {
  fun <T> parse(json: Reader, type: KType? = null): T? = JsonParser(json, opts).readValue(type) as T
  fun <T> parse(@Language("JSON") json: String, type: KType? = null) = parse(json.reader(), type) as T?
  fun <T> parse(json: InputStream, type: KType? = null) = parse(json.reader(), type) as T?

  fun render(o: Any?, out: Writer) = JsonRenderer(out, opts).render(o)
  fun render(o: Any?, out: OutputStream) = render(o, OutputStreamWriter(out))
  @Language("JSON") fun render(o: Any?): String = StringWriter().apply { use { render(o, it) } }.toString()
}

data class JsonOptions(
  val trimToNull: Boolean = false,
  val keys: JsonConverter<String> = JsonConverter(),
  val values: JsonConverter<Any?> = JsonConverter()
)

open class JsonConverter<T> {
  open fun to(o: T) = o
  open fun from(o: T) = o
}

class SnakeCase: JsonConverter<String>() {
  private val humps = "(?<=.)(?=\\p{Upper})".toRegex()
  override fun to(o: String) = o.replace(humps, "_").lowercase()
  override fun from(o: String) = o.split('_').joinToString("") { it.replaceFirstChar { it.uppercaseChar() } }.replaceFirstChar { it.lowercaseChar() }
}

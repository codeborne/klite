package klite.json

import org.intellij.lang.annotations.Language
import java.io.InputStream
import java.io.Reader
import kotlin.reflect.KType

// TODO: support these
annotation class JsonIgnore
annotation class JsonProperty(val value: String = "")

class JsonMapper(val opts: JsonOptions = JsonOptions()) {
  fun <T> parse(json: Reader, type: KType? = null): T? = JsonReader(json, opts).readValue(type) as T
  fun <T> parse(@Language("JSON") json: String, type: KType? = null) = parse(json.reader(), type) as T?
  fun <T> parse(json: InputStream, type: KType? = null) = parse(json.reader(), type) as T?
}

data class JsonOptions(
  val trimToNull: Boolean = false,
  val keyConverter: String.() -> String = { this },
  val valueConverter: (Any?) -> Any? = { it }
) {
  companion object {
    private val humps = "(?<=.)(?=\\p{Upper})".toRegex()
    val TO_SNAKE_CASE: String.() -> String = { replace(humps, "_").lowercase() }
    val FROM_SNAKE_CASE: String.() -> String = { split('_').joinToString("") { it.replaceFirstChar { it.uppercaseChar() } }.replaceFirstChar { it.lowercaseChar() } }
  }
}

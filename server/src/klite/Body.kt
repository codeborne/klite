package klite

import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface SupportsContentType {
  val contentType: String
}

class Accept(val contentTypes: String?) {
  val isRelaxed get() = contentTypes == null || contentTypes.contains("*") || contentTypes.startsWith("text/html,")
  operator fun invoke(contentType: String) = contentTypes?.contains(contentType) ?: true
  operator fun invoke(provider: SupportsContentType) = invoke(provider.contentType)
}

interface BodyRenderer: SupportsContentType {
  fun render(output: OutputStream, value: Any?) { throw UnsupportedOperationException() }
  fun render(e: HttpExchange, code: StatusCode, value: Any?) {
    e.startResponse(code, contentType = contentType).use { render(it, value) }
  }
}

interface BodyParser: SupportsContentType {
  @Deprecated("Use/implement the KType method")
  fun <T: Any> parse(input: InputStream, type: KClass<T>): T = throw UnsupportedOperationException()
  @Suppress("UNCHECKED_CAST")
  fun <T: Any> parse(input: InputStream, type: KType): T = parse(input, type.classifier as KClass<T>)
  fun <T> parse(e: HttpExchange, type: KType): T = e.requestStream.use { parse(it, type) }
}

class TextBodyRenderer(override val contentType: String = "text/plain"): BodyRenderer {
  override fun render(output: OutputStream, value: Any?) = output.write(value.toString().toByteArray())
}

class TextBodyParser(
  override val contentType: String = "text/plain"
): BodyParser {
  @Suppress("UNCHECKED_CAST")
  override fun <T: Any> parse(input: InputStream, type: KType): T {
    val s = input.reader().readText()
    return if (type == String::class) s as T else Converter.from(s, type)
  }
}

class FormUrlEncodedParser(override val contentType: String = MimeTypes.wwwForm): BodyParser {
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> parse(input: InputStream, type: KType): T = urlDecodeParams(input.reader().readText()) as T
}

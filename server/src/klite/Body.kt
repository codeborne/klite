package klite

import klite.annotations.TypeConverter
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

interface ContentTypeProvider {
  val contentType: String
}

interface BodyRenderer: ContentTypeProvider {
  fun render(output: OutputStream, value: Any?)
}

interface BodyParser: ContentTypeProvider {
  fun <T: Any> parse(input: InputStream, type: KClass<T>): T
}

class TextBodyRenderer(override val contentType: String = "text/plain"): BodyRenderer {
  override fun render(output: OutputStream, value: Any?) = output.write(value.toString().toByteArray())
}

class TextBodyParser(
  val typeConverter: TypeConverter = TypeConverter(),
  override val contentType: String = "text/plain"
): BodyParser {
  override fun <T: Any> parse(input: InputStream, type: KClass<T>): T {
    val s = input.readBytes().decodeToString()
    return if (type == String::class) s as T else typeConverter.fromString(s, type)
  }
}

class FormUrlEncodedParser(override val contentType: String = "application/x-www-form-urlencoded"): BodyParser {
  override fun <T : Any> parse(input: InputStream, type: KClass<T>): T = urlDecodeParams(input.readBytes().decodeToString()) as T
}

// TODO: multipart/form-data parser

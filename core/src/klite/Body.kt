package klite

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

class TextBody(override val contentType: String = "text/plain"): BodyRenderer, BodyParser {
  override fun render(output: OutputStream, value: Any?) = output.write(value.toString().toByteArray())

  override fun <T: Any> parse(input: InputStream, type: KClass<T>): T =
    if (type == String::class) input.readBytes().decodeToString() as T
    else throw UnsupportedOperationException("Cannot create $type")
}

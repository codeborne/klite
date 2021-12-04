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

class TextBodyRenderer(override val contentType: String = "text/plain"): BodyRenderer {
  override fun render(output: OutputStream, value: Any?) =
    output.write(value.toString().toByteArray())
}

interface BodyParser: ContentTypeProvider {
  fun <T: Any> parse(input: InputStream, type: KClass<T>): T
}

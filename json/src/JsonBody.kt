package klite.json

import klite.*
import klite.StatusCode.Companion.BadRequest
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KType

open class JsonBody(
  val json: JsonMapper = JsonMapper(),
  override val contentType: String = MimeTypes.json
): BodyParser, BodyRenderer, Extension {
  override fun <T: Any> parse(input: InputStream, type: KType): T = json.parse(input, type)!!
  override fun render(output: OutputStream, value: Any?) = TODO()

  override fun install(server: Server) = server.run {
    registry.register(json)
    errors.apply {
      on(JsonParseException::class, BadRequest)
    }
    renderers += this@JsonBody
    parsers += this@JsonBody
  }
}

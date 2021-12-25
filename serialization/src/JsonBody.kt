package klite.serialization

import klite.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

class JsonBody(
  val json: Json = Json {
    ignoreUnknownKeys = true
  },
  override val contentType: String = "application/json"
): BodyParser, BodyRenderer, Extension {
  override fun <T: Any> parse(input: InputStream, type: KClass<T>): T = json.decodeFromStream(serializer(type.java), input) as T
  override fun render(output: OutputStream, value: Any?) = json.encodeToStream(value, output)

  override fun install(server: Server) = server.run {
    registry.register(json)
    renderers += this@JsonBody
    parsers += this@JsonBody
  }
}

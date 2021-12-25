package klite.json

import klite.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import kotlin.reflect.KClass

class JsonBody(
  val json: Json = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
      contextual(LocalDateSerializer)
    }
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

object LocalDateSerializer: KSerializer<LocalDate> {
  override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

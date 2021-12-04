package klite.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import klite.BodyParser
import klite.BodyRenderer
import klite.HttpExchange
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

fun buildMapper() = JsonMapper.builder()
  .addModule(JavaTimeModule())
  .addModule(KotlinModule())
  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  .serializationInclusion(JsonInclude.Include.NON_NULL)
  .build()

class JsonBody(
  val mapper: ObjectMapper = buildMapper(),
  override val contentType: String = "application/json"
): BodyParser, BodyRenderer {
  override fun <T: Any> parse(input: InputStream, type: KClass<T>): T =
    mapper.readValue(input, type.java)

  override fun render(output: OutputStream, value: Any?) =
    mapper.writeValue(output, value)
}

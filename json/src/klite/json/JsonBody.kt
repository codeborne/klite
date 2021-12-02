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

fun buildMapper() = JsonMapper.builder()
  .addModule(JavaTimeModule())
  .addModule(KotlinModule())
  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  .serializationInclusion(JsonInclude.Include.NON_NULL)
  .build()

class JsonBody(
  val objectMapper: ObjectMapper = buildMapper()
): BodyParser, BodyRenderer {
  override fun parse(exchange: HttpExchange, contentType: String) {
    TODO("Not yet implemented")
  }

  override fun render(exchange: HttpExchange, value: Any?) {
    TODO("Not yet implemented")
  }
}

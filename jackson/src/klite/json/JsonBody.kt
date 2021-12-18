package klite.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jsonMapper
import klite.*
import klite.StatusCode.Companion.BadRequest
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

fun buildMapper() = jsonMapper {
  addModule(KotlinModule())
  addModule(JavaTimeModule())
  disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  serializationInclusion(JsonInclude.Include.NON_NULL)
}

class JsonBody(
  val json: JsonMapper = buildMapper(),
  override val contentType: String = "application/json"
): BodyParser, BodyRenderer, Extension {
  override fun <T: Any> parse(input: InputStream, type: KClass<T>): T = json.readValue(input, type.java)
  override fun render(output: OutputStream, value: Any?) = json.writeValue(output, value)

  override fun install(server: Server) {
    server.registry.register(json)
    server.errorHandler.apply {
      on(MissingKotlinParameterException::class, BadRequest)
      on(ValueInstantiationException::class, BadRequest)
    }
    server.addJson()
  }
}

fun RouterConfig.addJson() {
  val json = registry.require<JsonBody>()
  renderer(json)
  parser(json)
}

fun RouterConfig.jsonOnly() {
  renderers.clear()
  parsers.clear()
  addJson()
}

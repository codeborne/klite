package klite.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jsonMapper
import klite.*
import klite.StatusCode.Companion.BadRequest
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun buildMapper() = jsonMapper {
  addModule(KotlinModule())
  addModule(JavaTimeModule())
  disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  serializationInclusion(JsonInclude.Include.NON_NULL)
  addModule(SimpleModule().apply { addDeserializer(String::class.java, EmptyStringToNullDeserializer) })
//  withCoercionConfigDefaults {
//    it.acceptBlankAsEmpty = true
//    it.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull)
//  }
}

object EmptyStringToNullDeserializer: JsonDeserializer<String?>() {
  override fun deserialize(jsonParser: JsonParser, context: DeserializationContext?): String? =
    jsonParser.readValueAsTree<JsonNode>().asText().trimToNull()
}

class JsonBody(
  val json: JsonMapper = buildMapper(),
  override val contentType: String = "application/json"
): BodyParser, BodyRenderer, Extension {
  override fun <T: Any> parse(input: InputStream, type: KClass<T>): T = json.readValue(input, type.java)
  override fun <T: Any> parse(input: InputStream, type: KType): T = json.readValue(input,
    json.typeFactory.constructParametricType(type.java, *type.arguments.map { it.type!!.java }.toTypedArray()))

  override fun render(output: OutputStream, value: Any?) = json.writeValue(output, value)

  override fun install(server: Server) = server.run {
    registry.register(json)
    errors.apply {
      on(JsonParseException::class, BadRequest)
      on(MismatchedInputException::class, BadRequest)
      on(ValueInstantiationException::class) { e, _ -> handleValueInstantiation(e) }
      on(MissingKotlinParameterException::class) { e, _ -> handleMissingParameter(e) }
    }
    renderers += this@JsonBody
    parsers += this@JsonBody
  }

  internal fun handleValueInstantiation(e: ValueInstantiationException): ErrorResponse {
    val message = if (e.cause is IllegalArgumentException) e.cause!!.message else e.message
    logger().error(e.toString())
    return ErrorResponse(BadRequest, message)
  }

  internal fun handleMissingParameter(e: MissingKotlinParameterException): ErrorResponse {
    val message = "${e.parameter.name} is required"
    logger().error("$message: " + e.message)
    return ErrorResponse(BadRequest, message)
  }
}

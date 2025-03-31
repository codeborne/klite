package klite.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import klite.*
import klite.StatusCode.Companion.BadRequest
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KType

fun kliteJsonMapper(kotlinModule: KotlinModule = kotlinModule(), javaTimeModule: JavaTimeModule = JavaTimeModule(), modifier: JsonMapper.Builder.() -> Unit = {}) = jsonMapper {
  addModule(kotlinModule)
  addModule(javaTimeModule)
  disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
  serializationInclusion(JsonInclude.Include.NON_NULL)
  addModule(SimpleModule().apply {
    addDeserializer(String::class.java, EmptyStringToNullDeserializer)
    addSerializer(Enum::class.java, EnumToStringSerializer)
    addConverterDeserializers()
  })
  withCoercionConfigDefaults {
    it.acceptBlankAsEmpty = true
    it.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull)
  }
  modifier()
}

object EmptyStringToNullDeserializer: JsonDeserializer<String?>() {
  override fun deserialize(jsonParser: JsonParser, context: DeserializationContext?): String? =
    jsonParser.valueAsString?.trimToNull()
}

object EnumToStringSerializer: JsonSerializer<Enum<*>>() {
  override fun serialize(value: Enum<*>, gen: JsonGenerator, serializers: SerializerProvider) {
    gen.writeString(value.toString())
  }
}

fun SimpleModule.addConverterDeserializers() = Converter.forEach { type, converter ->
  @Suppress("UNCHECKED_CAST")
  addDeserializer(type.java as Class<Any>, object: JsonDeserializer<Any>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext?): Any? =
      jsonParser.valueAsString?.trimToNull()?.let { converter.invoke(it) }
  })
}

fun JsonMapper.typeOf(type: KType): JavaType = typeFactory.constructParametricType(type.java, *type.arguments.map { it.type!!.java }.toTypedArray())

open class JsonBody(
  val json: JsonMapper = kliteJsonMapper(),
  override val contentType: String = MimeTypes.json
): BodyParser, BodyRenderer, Extension {
  override fun <T: Any> parse(input: InputStream, type: KType): T = json.readValue(input, json.typeOf(type))

  override fun render(output: OutputStream, value: Any?) = json.writeValue(output, value)

  override fun install(server: Server) = server.run {
    registry.register(json)
    errors.apply {
      on(JsonParseException::class, BadRequest)
      on(MismatchedInputException::class, BadRequest)
      on(ValueInstantiationException::class) { e -> handleValueInstantiation(e) }
      on(MissingKotlinParameterException::class) { e -> handleMissingParameter(e) }
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

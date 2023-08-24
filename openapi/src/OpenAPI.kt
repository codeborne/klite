package klite.openapi

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.media.Schema.AccessMode
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import klite.*
import klite.RequestMethod.GET
import klite.StatusCode.Companion.NoContent
import klite.StatusCode.Companion.OK
import klite.annotations.*
import java.math.BigDecimal
import java.net.URI
import java.net.URL
import java.time.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.*

/**
 * Adds an /openapi endpoint to the context, listing all the routes.
 * This handler will try to gather all the information about request/parameters/response automatically, but you can use Swagger annotations to specify more details.
 * - Pass [@OpenAPIDefinition][io.swagger.v3.oas.annotations.OpenAPIDefinition] or only [@Info][io.swagger.v3.oas.annotations.info.Info] annotation to this function to specify general info
 * - Pass [@SecurityScheme][io.swagger.v3.oas.annotations.security.SecurityScheme] to define authorization
 * - Use [@Operation][io.swagger.v3.oas.annotations.Operation] annotation to describe each route
 * - Use [@SecurityRequirement][io.swagger.v3.oas.annotations.security.SecurityRequirement] annotations to reference security requirements of the route
 * - Use [@RequestBody][io.swagger.v3.oas.annotations.parameters.RequestBody] annotations to define the request body
 * - Use [@ApiResponse][io.swagger.v3.oas.annotations.responses.ApiResponse] annotations to define one or many possible responses
 * - Use [@Hidden][io.swagger.v3.oas.annotations.Hidden] to hide a route from the spec
 * - [@Parameter][io.swagger.v3.oas.annotations.Parameter] annotation can be used on method parameters directly
 * - [@Tag][io.swagger.v3.oas.annotations.tags.Tag] annotation is supported on route classes for grouping of routes
 */
fun Router.openApi(path: String = "/openapi", annotations: List<Annotation> = emptyList()) {
  add(Route(GET, path.toRegex(), annotations.toList()) { generateOpenAPI() })
}

context(HttpExchange)
internal fun Router.generateOpenAPI() = mapOf(
  "openapi" to "3.1.0",
  "info" to route.findAnnotation<Info>()?.toNonEmptyValues(),
  "servers" to listOf(mapOf("url" to fullUrl(prefix))),
  "tags" to toTags(routes),
  "components" to mapOfNotNull(
    "securitySchemes" to route.findAnnotations<SecurityScheme>().associate { s ->
      s.name to s.toNonEmptyValues { it.name != "paramName" }.let { it + ("name" to s.paramName) }
    }.takeIf { it.isNotEmpty() }
  ).takeIf { it.isNotEmpty() },
  "paths" to routes.filter { !it.hasAnnotation<Hidden>() }.groupBy { pathParamRegexer.toOpenApi(it.path) }.mapValues { (_, routes) ->
    routes.associate(::toOperation)
  },
) + (route.findAnnotation<OpenAPIDefinition>()?.let {
  it.toNonEmptyValues() + ("security" to it.security.toList().toSecurity())
} ?: emptyMap())

internal fun toTags(routes: List<Route>) = routes.asSequence()
  .map { it.handler }
  .filterIsInstance<FunHandler>()
  .flatMap { it.instance::class.findAnnotations<Tag>().map { it.toNonEmptyValues() }.ifEmpty { listOf(mapOf("name" to it.instance::class.simpleName)) } }
  .toSet()

internal fun toOperation(route: Route): Pair<String, Any> {
  val op = route.findAnnotation<Operation>()
  val funHandler = route.handler as? FunHandler
  return (op?.method?.trimToNull() ?: route.method.name).lowercase() to mapOf(
    "operationId" to route.handler.let { (if (it is FunHandler) it.instance::class.simpleName + "." + it.f.name else it::class.simpleName) },
    "tags" to listOfNotNull(funHandler?.let { it.instance::class.annotation<Tag>()?.name ?: it.instance::class.simpleName }),
    "parameters" to funHandler?.let {
      it.params.filter { it.source != null }.map { p -> toParameter(p, op) }
    },
    "requestBody" to toRequestBody(route, route.findAnnotation<RequestBody>() ?: op?.requestBody),
    "responses" to toResponsesByCode(route, op, funHandler?.f?.returnType),
    "security" to (op?.security?.toList() ?: route.findAnnotations<SecurityRequirement>()).toSecurity()
  ) + (op?.let { it.toNonEmptyValues { it.name !in setOf("method", "requestBody", "responses") } } ?: emptyMap())
}

fun toParameter(p: Param, op: Operation? = null) = mapOf(
  "name" to p.name,
  "required" to (!p.p.isOptional && !p.p.type.isMarkedNullable),
  "in" to toParameterIn(p.source),
  "schema" to p.p.type.toJsonSchema(response = true),
) + ((p.p.findAnnotation<Parameter>() ?: op?.parameters?.find { it.name == p.name })?.toNonEmptyValues() ?: emptyMap())

private fun toParameterIn(paramAnnotation: Annotation?) = when(paramAnnotation) {
  is HeaderParam -> ParameterIn.HEADER
  is QueryParam -> ParameterIn.QUERY
  is PathParam -> ParameterIn.PATH
  is CookieParam -> ParameterIn.COOKIE
  else -> null
}

private fun KType.toJsonSchema(response: Boolean = false): Map<String, Any>? {
  val cls = classifier as? KClass<*> ?: return null
  val jsonType = when {
    cls == Nothing::class -> "null"
    cls == Boolean::class -> "boolean"
    cls == BigDecimal::class || cls == Decimal::class || cls == Float::class || cls == Double::class -> "number"
    cls.isSubclassOf(Number::class) -> "integer"
    cls.isSubclassOf(Array::class) || cls.isSubclassOf(Iterable::class) -> "array"
    cls.isSubclassOf(CharSequence::class) || Converter.supports(cls) -> "string"
    else -> "object"
  }
  val jsonStringFormat = when (cls) {
    LocalDate::class, Date::class -> "date"
    LocalTime::class -> "time"
    Instant::class, LocalDateTime::class -> "date-time"
    Period::class, Duration::class -> "duration"
    URI::class, URL::class -> "uri"
    UUID::class -> "uuid"
    else -> null
  }
  return mapOfNotNull(
    "type" to jsonType,
    "format" to jsonStringFormat,
    "items" to if (jsonType == "array") arguments.firstOrNull()?.type?.toJsonSchema() else null,
    "enum" to if (cls.isSubclassOf(Enum::class)) cls.java.enumConstants.toList() else null,
    "properties" to if (jsonType == "object") cls.publicProperties.associate { it.name to it.returnType.toJsonSchema(response) }.takeIf { it.isNotEmpty() } else null,
    "required" to if (jsonType == "object") cls.publicProperties.filter { p ->
      !p.returnType.isMarkedNullable && (response || cls.primaryConstructor?.parameters?.find { it.name == p.name }?.isOptional != true)
    }.map { it.name }.toSet().takeIf { it.isNotEmpty() } else null
  )
}

private fun toRequestBody(route: Route, annotation: RequestBody?): Map<String, Any?>? {
  val bodyParam = (route.handler as? FunHandler)?.params?.find { it.p.kind != INSTANCE && it.source == null && it.cls.java.packageName != "klite" }?.p
  val requestBody = annotation?.toNonEmptyValues() ?: HashMap()
  if (annotation != null && annotation.content.isNotEmpty())
    requestBody["content"] = annotation.content.associate {
      val content = it.toNonEmptyValues { it.name != "mediaType" }
      if (it.schema.implementation != Void::class.java) content["schema"] = it.schema.implementation.createType().toJsonSchema()
      else if (it.array.schema.implementation != Void::class.java) content["schema"] = Array::class.createType(arguments = listOf(KTypeProjection(null, it.array.schema.implementation.createType()))).toJsonSchema()
      it.mediaType to content
    }
  if (bodyParam != null) requestBody.putIfAbsent("content", bodyParam.type.toJsonContent())
  if (requestBody.isEmpty()) return null
  requestBody.putIfAbsent("required", bodyParam == null || !bodyParam.isOptional)
  return requestBody
}

private fun toResponsesByCode(route: Route, op: Operation?, returnType: KType?): Map<StatusCode, Any?> {
  val responses = LinkedHashMap<StatusCode, Any?>()
  if (returnType?.classifier == Unit::class) responses[NoContent] = mapOf("description" to "No content")
  else if (op?.responses?.isEmpty() != false) responses[OK] = mapOfNotNull("description" to "OK", "content" to returnType?.toJsonContent(response = true))
  (route.findAnnotations<ApiResponse>() + (op?.responses ?: emptyArray())).forEach {
    responses[StatusCode(it.responseCode.toInt())] = it.toNonEmptyValues { it.name != "responseCode" }
  }
  return responses
}

private fun KType.toJsonContent(response: Boolean = false) = mapOf(MimeTypes.json to mapOf("schema" to toJsonSchema(response)))

private fun List<SecurityRequirement>.toSecurity() = map { mapOf(it.name to it.scopes.toList()) }.takeIf { it.isNotEmpty() }

internal fun <T: Annotation> T.toNonEmptyValues(filter: (KProperty1<T, *>) -> Boolean = { true }): MutableMap<String, Any?> = HashMap<String, Any?>().also { map ->
  publicProperties.filter(filter).forEach { p ->
    when(val v = p.valueOf(this)) {
      "", false, 0, Int.MAX_VALUE, Int.MIN_VALUE, 0.0, Void::class.java, AccessMode.AUTO -> null
      is Enum<*> -> v.takeIf { v.name != "DEFAULT" }
      is Annotation -> v.toNonEmptyValues().takeIf { it.isNotEmpty() }
      is Array<*> -> v.map { (it as? Annotation)?.toNonEmptyValues() ?: it }.takeIf { it.isNotEmpty() }
      else -> v
    }?.let { map[p.name] = it }
  }}

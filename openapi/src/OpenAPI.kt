package klite.openapi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema.AccessMode
import io.swagger.v3.oas.annotations.tags.Tag
import klite.*
import klite.StatusCode.Companion.NoContent
import klite.StatusCode.Companion.OK
import klite.annotations.*
import java.math.BigDecimal
import java.net.URI
import java.net.URL
import java.time.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

// Spec: https://swagger.io/specification/
// Sample: https://github.com/OAI/OpenAPI-Specification/blob/main/examples/v3.0/api-with-examples.json

/**
 * Adds an /openapi endpoint to the context, listing all the routes.
 * - Use @Operation swagger annotation to describe the routes.
 * - @Parameter annotation can be used on method parameters directly.
 * - @Tag annotation is supported on route classes for grouping of routes.
 */
fun Router.openApi(path: String = "/openapi", title: String = "API", version: String = "1.0.0") {
  get(path) {
    mapOf(
      "openapi" to "3.0.0",
      "info" to mapOf("title" to title, "version" to version),
      "servers" to listOf(mapOf("url" to fullUrl(prefix))),
      "tags" to toTags(routes),
      "paths" to routes.groupBy { pathParamRegexer.toOpenApi(it.path) }.mapValues { (_, routes) ->
        routes.associate(::toOperation)
      }
    )
  }
}

internal fun toTags(routes: List<Route>) = routes.asSequence()
  .map { it.handler }
  .filterIsInstance<FunHandler>()
  .map { it.instance::class.annotation<Tag>()?.toNonEmptyValues() ?: mapOf("name" to it.instance::class.simpleName) }
  .toSet()

internal fun toOperation(route: Route): Pair<String, Any> {
  val op = route.annotation<Operation>()
  val funHandler = route.handler as? FunHandler
  val returnType = funHandler?.f?.returnType
  return (op?.method?.trimToNull() ?: route.method.name).lowercase() to mapOf(
    "operationId" to route.handler.let { (if (it is FunHandler) it.instance::class.simpleName + "." + it.f.name else it::class.simpleName) },
    "tags" to listOfNotNull(funHandler?.let { it.instance::class.annotation<Tag>()?.name ?: it.instance::class.simpleName }),
    "parameters" to funHandler?.let {
      it.params.filter { it.source != null }.map { p -> toParameter(p, op) }
    },
    "requestBody" to findRequestBody(route),
    "responses" to if (returnType?.classifier == Unit::class) mapOf(NoContent.value to mapOf("description" to "No content"))
                   else mapOf(OK.value to mapOfNotNull("description" to "OK", "content" to returnType?.toJsonContent()))
  ) + (op?.let { it.toNonEmptyValues { it.name != "method" } + mapOf(
    "responses" to op.responses.associate { it.responseCode to it.toNonEmptyValues { it.name != "responseCode" } }.takeIf { it.isNotEmpty() }
  ) } ?: emptyMap())
}

fun toParameter(p: Param, op: Operation? = null) = mapOf(
  "name" to p.name,
  "required" to (!p.p.isOptional && !p.p.type.isMarkedNullable),
  "in" to toParameterIn(p.source),
  "schema" to p.p.type.toJsonSchema(),
) + ((p.p.findAnnotation<Parameter>() ?: op?.parameters?.find { it.name == p.name })?.toNonEmptyValues() ?: emptyMap())

private fun toParameterIn(paramAnnotation: Annotation?) = when(paramAnnotation) {
  is HeaderParam -> ParameterIn.HEADER
  is QueryParam -> ParameterIn.QUERY
  is PathParam -> ParameterIn.PATH
  is CookieParam -> ParameterIn.COOKIE
  else -> null
}

private fun KType.toJsonSchema(): Map<String, Any>? {
  val cls = classifier as? KClass<*> ?: return null
  val jsonType = when (cls) {
    Nothing::class -> "null"
    Boolean::class -> "boolean"
    Number::class -> "integer"
    BigDecimal::class, Decimal::class, Float::class, Double::class -> "number"
    else -> if (cls == String::class || Converter.supports(cls)) "string" else "object"
  }
  val jsonFormat = when (cls) {
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
    "format" to jsonFormat,
    "enum" to if (cls.isSubclassOf(Enum::class)) cls.java.enumConstants.toList() else null,
    "properties" to if (jsonType == "object") cls.publicProperties.associate { it.name to it.returnType.toJsonSchema() }.takeIf { it.isNotEmpty() } else null,
    "required" to if (jsonType == "object") cls.publicProperties.filter { !it.returnType.isMarkedNullable }.map { it.name }.toSet().takeIf { it.isNotEmpty() } else null
  )
}

private fun findRequestBody(route: Route) = (route.handler as? FunHandler)?.params?.
  find { it.p.kind != INSTANCE && it.source == null && it.cls.java.packageName != "klite" }?.p?.toRequestBody()

private fun KParameter.toRequestBody() = mapOf("content" to type.toJsonContent())
private fun KType.toJsonContent() = mapOf(MimeTypes.json to mapOf("schema" to toJsonSchema()))

internal fun <T: Annotation> T.toNonEmptyValues(filter: (KProperty1<T, *>) -> Boolean = { true }): Map<String, Any?> =
  publicProperties.filter(filter).associate { it.name to when(val v = it.valueOf(this)) {
    "", false, 0, Int.MAX_VALUE, Int.MIN_VALUE, 0.0, Void::class.java, AccessMode.AUTO -> null
    is Enum<*> -> v.takeIf { v.name != "DEFAULT" }
    is Annotation -> v.toNonEmptyValues().takeIf { it.isNotEmpty() }
    is Array<*> -> v.map { (it as? Annotation)?.toNonEmptyValues() ?: it }.takeIf { it.isNotEmpty() }
    else -> v
  }}.filterValues { it != null }

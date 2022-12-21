package klite.annotations

import klite.*
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.*
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

@Target(CLASS) annotation class Path(val value: String)
@Target(FUNCTION) annotation class GET(val value: String = "")
@Target(FUNCTION) annotation class POST(val value: String = "")
@Target(FUNCTION) annotation class PUT(val value: String = "")
@Target(FUNCTION) annotation class PATCH(val value: String = "")
@Target(FUNCTION) annotation class DELETE(val value: String = "")
@Target(FUNCTION) annotation class OPTIONS(val value: String = "")

@Target(VALUE_PARAMETER) annotation class PathParam
@Target(VALUE_PARAMETER) annotation class QueryParam(val value: String = "")
@Target(VALUE_PARAMETER) annotation class BodyParam(val value: String = "")
@Target(VALUE_PARAMETER) annotation class HeaderParam(val value: String = "")
@Target(VALUE_PARAMETER) annotation class CookieParam(val value: String = "")
@Target(VALUE_PARAMETER) annotation class SessionParam(val value: String = "")
@Target(VALUE_PARAMETER) annotation class AttrParam(val value: String = "")

/**
 * Adds all annotated methods as routes, sorted by path (matching more specific paths first).
 * Routes can also implement Before/After interfaces.
 *
 * Use the @XXXParam annotations to bind specific types of params.
 * Non-annotated binding of well known classes is possible, like [HttpExchange] and [Session].
 * Non-annotated custom class is interpreted as the whole POST/PUT body, e.g. a data class deserialized from json.
 */
fun Router.annotated(routes: Any) {
  val cls = routes::class
  val path = cls.annotation<Path>()?.value ?: ""
  val classDecorators = mutableListOf<Decorator>()
  if (routes is Before) classDecorators += routes.toDecorator()
  if (routes is After) classDecorators += routes.toDecorator()
  cls.functions.asSequence().mapNotNull { f ->
    val a = f.kliteAnnotation ?: return@mapNotNull null
    val method = RequestMethod.valueOf(a.annotationClass.simpleName!!)
    val subPath = a.annotationClass.members.first().call(a) as String
    subPath to Route(method, pathParamRegexer.from(path + subPath), f.annotations + cls.annotations, classDecorators.wrap(toHandler(routes, f)))
  }.sortedBy { it.first.replace(':', '~') }.forEach { add(it.second) }
}

inline fun <reified T: Any> Router.annotated() = annotated(require<T>())

private val packageName = GET::class.java.packageName
private val KAnnotatedElement.kliteAnnotation get() = annotations.filter { it.annotationClass.java.packageName == packageName }
  .let { if (it.size > 1) error("$this cannot have multiple klite annotations: $it") else it.firstOrNull() }

internal fun toHandler(instance: Any, f: KFunction<*>): Handler {
  val params = f.parameters.map(::Param)
  return {
    try {
      val args = params.associate { p -> p.param to paramValue(p, instance) }.filter { !it.key.isOptional || it.value != null }
      f.callSuspendBy(args)
    } catch (e: InvocationTargetException) {
      throw e.targetException
    }
  }
}

private class Param(val param: KParameter) {
  val cls = param.type.classifier as KClass<*>
  val annotation: Annotation? = param.kliteAnnotation
  val name: String = annotation?.takeIf { it !is PathParam }?.value ?: param.name ?: ""

  val Annotation.value: String? get() = (javaClass.getMethod("value").invoke(this) as String).takeIf { it.isNotEmpty() }
}

private fun HttpExchange.paramValue(p: Param, instance: Any) =
  if (p.param.kind == INSTANCE) instance
  else if (p.cls == HttpExchange::class) this
  else if (p.cls == Session::class) session
  else if (p.cls == InputStream::class) requestStream
  else {
    fun String.toType() = Converter.from<Any>(this, p.param.type)
    when (p.annotation) {
      is PathParam -> path(p.name)?.toType()
      is QueryParam -> query(p.name)?.toType()
      is HeaderParam -> header(p.name)?.toType()
      is CookieParam -> cookie(p.name)?.toType()
      is SessionParam -> session[p.name]?.toType()
      is AttrParam -> attr(p.name)
      is BodyParam -> body<Any?>(p.name)?.let { if (it is String) it.toType() else it }
      else -> body(p.param.type)
    }
  }

inline fun <reified T: Annotation> KClass<*>.annotation(): T? = java.getAnnotation(T::class.java)
inline fun <reified T: Annotation> KFunction<*>.annotation(): T? = javaMethod!!.getAnnotation(T::class.java)

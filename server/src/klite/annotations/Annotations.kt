package klite.annotations

import klite.*
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

@Target(CLASS) annotation class Path(val value: String)
@Target(FUNCTION) annotation class GET(val value: String = "")
@Target(FUNCTION) annotation class POST(val value: String = "")
@Target(FUNCTION) annotation class PUT(val value: String = "")
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
 */
fun Router.annotated(routes: Any) {
  val cls = routes::class
  val path = cls.annotation<Path>()?.value ?: ""
  val classDecorators = mutableListOf<Decorator>()
  if (routes is Before) classDecorators += routes.toDecorator()
  if (routes is After) classDecorators += routes.toDecorator()
  cls.functions.asSequence().mapNotNull { f ->
    val a = f.annotations.firstOrNull() ?: return@mapNotNull null
    val method = RequestMethod.valueOf(a.annotationClass.simpleName!!)
    val subPath = a.annotationClass.members.first().call(a) as String
    subPath to Route(method, pathParamRegexer.from(path + subPath), f.annotations + cls.annotations, classDecorators.wrap(toHandler(routes, f)))
  }.sortedBy { it.first.replace(':', '~') }.forEach { add(it.second) }
}

inline fun <reified T: Any> Router.annotated() = annotated(require<T>())

internal fun toHandler(instance: Any, f: KFunction<*>): Handler {
  val params = f.parameters
  return {
    try {
      val args = params.associateWith { p ->
        if (p.kind == INSTANCE) instance
        else if (p.type.classifier == HttpExchange::class) this
        else if (p.type.classifier == Session::class) session
        else if (p.type.classifier == InputStream::class) requestStream
        else {
          val name = p.name!!
          fun String.toType() = Converter.from(this, p.type.classifier as KClass<*>) // TODO: support for KType in Converter
          when (val a = p.annotations.firstOrNull()) {
            is PathParam -> path(name)?.toType()
            is QueryParam -> query(a.value.ifEmpty { name })?.toType()
            is BodyParam -> body(a.value.ifEmpty { name })?.toType()
            is HeaderParam -> header(a.value.ifEmpty { name })?.toType()
            is CookieParam -> cookie(a.value.ifEmpty { name })?.toType()
            is SessionParam -> session[a.value.ifEmpty { name }]?.toType()
            is AttrParam -> attr(a.value.ifEmpty { name })
            else -> body(p.type)
          }
        }
      }.filter { !it.key.isOptional || it.value != null }
      f.callSuspendBy(args)
    } catch (e: InvocationTargetException) {
      throw e.targetException
    }
  }
}

inline fun <reified T: Annotation> KClass<*>.annotation(): T? = java.getAnnotation(T::class.java)
inline fun <reified T: Annotation> KFunction<*>.annotation(): T? = javaMethod!!.getAnnotation(T::class.java)

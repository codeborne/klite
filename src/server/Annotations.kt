package server

import java.lang.reflect.InvocationTargetException
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

@Target(CLASS) annotation class Path(val value: String)
@Target(FUNCTION) annotation class GET(val value: String = "")
@Target(FUNCTION) annotation class POST(val value: String = "")
@Target(FUNCTION) annotation class PUT(val value: String = "")
@Target(FUNCTION) annotation class DELETE(val value: String = "")
@Target(FUNCTION) annotation class OPTIONS(val value: String = "")

// TODO annotation class Body
@Target(VALUE_PARAMETER) annotation class PathParam
@Target(VALUE_PARAMETER) annotation class QueryParam
@Target(VALUE_PARAMETER) annotation class HeaderParam
@Target(VALUE_PARAMETER) annotation class AttributeParam

fun Server.routesFrom(routes: Any) {
  val path = routes::class.annotation<Path>()?.value ?: error("@Path is missing")
  context(path) {
    routes::class.functions.forEach { f ->
      val annotation = f.annotations.firstOrNull() ?: return@forEach
      val method = RequestMethod.valueOf(annotation.annotationClass.simpleName!!)
      val subPath = annotation.annotationClass.members.first().call(annotation) as String
      add(Route(method, pathParamRegexer.from(subPath), toHandler(routes, f)))
    }
  }
}

fun toHandler(instance: Any, f: KFunction<*>): Handler {
  val params = f.parameters
  return {
    try {
      val args = Array(params.size) { i ->
        val p = params[i]
        val a = p.annotations.firstOrNull()
        if (p.kind == INSTANCE) instance
        else if (p.type.classifier == HttpExchange::class) this
        else if (a is PathParam) path(p.name!!)
        else if (a is QueryParam) query(p.name!!)
        else if (a is HeaderParam) requestHeaders[p.name]
        else if (a is AttributeParam) getAttribute(p.name)
        else null
        // TODO: convert to correct type
      }
      f.callSuspend(*args)
    } catch (e: InvocationTargetException) {
      throw e.targetException
    }
  }
}

inline fun <reified T: Annotation> KClass<*>.annotation(): T? = java.getAnnotation(T::class.java)
inline fun <reified T: Annotation> KFunction<*>.annotation(): T? = javaMethod!!.getAnnotation(T::class.java)

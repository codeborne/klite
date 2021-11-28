package server

import com.sun.net.httpserver.HttpExchange
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod

annotation class Path(val value: String)
annotation class GET(val value: String = "")
// TODO annotation class POST(val value: String = "")

// TODO annotation class QueryParam
// TODO annotation class PathParam
annotation class HeaderParam
annotation class AttributeParam

fun Server.routesFrom(routes: Any) {
  val path = routes::class.annotation<Path>()?.value ?: ""
  routes::class.memberFunctions.forEach { f ->
    val get = f.annotation<GET>()
    if (get != null) route(path + get.value, toHandler(routes, f))
  }
}

fun toHandler(instance: Any, f: KFunction<*>): Handler {
  val params = f.parameters
  return { exchange ->
    try {
      val args = Array(params.size) { i ->
        val p = params[i]
        val a = p.annotations.firstOrNull()
        if (p.kind == INSTANCE) instance
        else if (p.type.classifier == HttpExchange::class) exchange
        else if (a is HeaderParam) exchange.requestHeaders[p.name]
        else if (a is AttributeParam) exchange.getAttribute(p.name)
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

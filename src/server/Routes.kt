package server

import com.sun.net.httpserver.HttpExchange
import java.lang.reflect.Method

annotation class Path(val value: String)
annotation class GET(val value: String = "")
// TODO annotation class POST(val value: String = "")

// TODO annotation class QueryParam
// TODO annotation class PathParam
annotation class HeaderParam
annotation class AttributeParam

fun toHandler(instance: Any, method: Method): Handler {
  val params = method.parameters
  return { exchange -> method.invoke(instance, *params.map { p ->
    if (p.type == HttpExchange::class.java) exchange
    else if (p.isAnnotationPresent(HeaderParam::class.java)) exchange.requestHeaders[p.name]
    else if (p.isAnnotationPresent(AttributeParam::class.java)) exchange.getAttribute(p.name)
    else null
    // TODO: convert to correct type
  }.toTypedArray())}
}

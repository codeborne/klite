package server

import com.sun.net.httpserver.HttpExchange
import java.lang.reflect.Method

annotation class Path(val value: String)
annotation class GET(val value: String = "")

fun toHandler(instance: Any, method: Method): Handler {
  val params = method.parameters
  return { exchange -> method.invoke(instance, *params.map { p ->
    if (p.type == HttpExchange::class.java) exchange
    else null
  }.toTypedArray())}
}

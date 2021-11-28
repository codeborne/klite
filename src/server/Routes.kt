package server

import java.lang.reflect.Method

annotation class Path(val value: String)
annotation class GET(val value: String = "")

fun toHandler(instance: Any, method: Method): Handler {
  return { exchange -> method.invoke(instance) }
}

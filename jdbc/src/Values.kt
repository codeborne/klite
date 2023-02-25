package klite.jdbc

import klite.PropValue
import klite.create
import java.sql.ResultSet
import kotlin.reflect.KClass

inline fun <reified T: Any> ResultSet.create(vararg provided: PropValue<T>) = create(T::class, *provided)

fun <T: Any> ResultSet.create(type: KClass<T>, vararg provided: PropValue<T>): T {
  val extraArgs = provided.associate { it.first.name to it.second }
  return type.create {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name!!]
    else if (it.isOptional) getOptional(it.name!!, it.type)
    else get(it.name!!, it.type)
  }
}

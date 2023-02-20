package klite.jdbc

import klite.PropValue
import klite.fromValues
import klite.toValues
import klite.toValuesSkipping
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

inline fun <T: Any> T.toValues(vararg provided: PropValue<T>) = toValues(*provided)
inline fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(*skip)

inline fun <reified T: Any> ResultSet.fromValues(vararg provided: PropValue<T>) = fromValues(T::class, *provided)
inline fun <reified T: Any> Map<String, Any?>.fromValues() = fromValues(T::class) { JdbcConverter.from(get(it.name), it.type) }

fun <T: Any> ResultSet.fromValues(type: KClass<T>, vararg provided: PropValue<T>): T {
  val extraArgs = provided.associate { it.first.name to it.second }
  return extraArgs.fromValues(type) {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name!!]
    else if (it.isOptional) getOptional(it.name!!, it.type)
    else get(it.name!!, it.type)
  }
}

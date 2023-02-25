package klite.jdbc

import klite.PropValue
import klite.create
import klite.toValues
import klite.toValuesSkipping
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@Deprecated("use type from klite-core", ReplaceWith("klite.PropValue<T>", "klite.PropValue"))
typealias PropValue<T> = klite.PropValue<T>

@Deprecated("use function from klite-core", ReplaceWith("this.toValues<T>(*provided)", "klite.toValues"))
inline fun <T: Any> T.toValues(vararg provided: PropValue<T>) = toValues(*provided)
@Deprecated("use function from klite-core", ReplaceWith("this.toValuesSkipping<T>(*skip)", "klite.toValuesSkipping"))
inline fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(*skip)

inline fun <reified T: Any> ResultSet.fromValues(vararg provided: PropValue<T>) = fromValues(T::class, *provided)
fun <T: Any> ResultSet.fromValues(type: KClass<T>, vararg provided: PropValue<T>): T {
  val extraArgs = provided.associate { it.first.name to it.second }
  return type.create {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name!!]
    else if (it.isOptional) getOptional(it.name!!, it.type)
    else get(it.name!!, it.type)
  }
}

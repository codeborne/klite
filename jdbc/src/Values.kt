package klite.jdbc

import java.sql.ResultSet
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

typealias PropValue<T> = Pair<KProperty1<T, *>, *>

fun <T: Any> T.toValues(vararg provided: PropValue<T>): Map<String, Any?> {
  val values = provided.associate { it.first.name to it.second }
  return toValuesSkipping(values.keys) + values
}

fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(skip.map { it.name }.toSet())

@Suppress("UNCHECKED_CAST")
private fun <T: Any> T.toValuesSkipping(skipNames: Set<String>): Map<String, Any?> =
  toValues((this::class.memberProperties as Iterable<KProperty1<T, *>>).filter { it.name !in skipNames })

fun <T: Any> T.toValues(props: Iterable<KProperty1<T, *>>): Map<String, Any?> =
  props.filter { it.visibility == PUBLIC && it.javaField != null }.associate { it.name to it.get(this) }

inline fun <reified T: Any> ResultSet.fromValues(vararg provided: PropValue<T>) = fromValues(T::class, *provided)
inline fun <reified T: Any> Map<String, Any?>.fromValues() = fromValues(T::class)

fun <T: Any> ResultSet.fromValues(type: KClass<T>, vararg provided: PropValue<T>): T {
  val extraArgs = provided.associate { it.first.name to it.second }
  return extraArgs.fromValues(type) {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name!!]
    else if (it.isOptional) getOptional(it.name!!, it.type)
    else get(it.name!!, it.type)
  }
}

fun <T: Any> Map<String, Any?>.fromValues(type: KClass<T>, getValue: (KParameter) -> Any? = { JdbcConverter.from(get(it.name), it.type) }): T {
  val constructor = type.primaryConstructor!!
  val args = constructor.parameters.associateWith { getValue(it) }.filterNot { it.key.isOptional && it.value == null }
  return try {
    constructor.callBy(args)
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException("Cannot create $type using " + args.mapKeys { it.key.name }, e)
  }
}

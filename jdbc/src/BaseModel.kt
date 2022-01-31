package klite.jdbc

import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

interface BaseModel {
  val id: UUID
}

inline fun <reified T: Any> T.toValues(vararg values: Pair<KProperty1<T, *>, Any?>): Map<String, Any?> {
  val provided = mapOf(*values)
  return toValuesSkipping(provided.keys) + provided.mapKeys { it.key.name }
}

inline fun <reified T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(setOf(*skip))

inline fun <reified T: Any> T.toValuesSkipping(skip: Set<KProperty1<T, *>>): Map<String, Any?> =
  toValues(T::class.memberProperties - skip)

fun <T: Any> T.toValues(props: Iterable<KProperty1<T, *>>): Map<String, Any?> =
  props.filter { it.javaField != null }.associate { it.name to it.javaField?.apply { trySetAccessible() }?.get(this) }

inline fun <reified T: Any> ResultSet.fromValues(vararg values: Pair<KProperty1<T, *>, Any?>) = fromValues(T::class, *values)

fun <T: Any> ResultSet.fromValues(type: KClass<T>, vararg values: Pair<KProperty1<T, *>, Any?>) = type.primaryConstructor!!.let { constructor ->
  val extraArgs = values.associate { it.first.name to it.second }
  val args = constructor.parameters.associateWith {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name] else JdbcConverter.from(getObject(it.name), it.type)
  }
  constructor.callBy(args)
}

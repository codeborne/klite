package klite.jdbc

import java.sql.ResultSet
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

fun <T: Any> T.toValues(vararg provided: PropValue<T, *>): Map<String, Any?> {
  val values = provided.associate { it.property.name to it.value }
  return toValuesSkipping(values.keys) + values
}

fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(skip.map { it.name }.toSet())

@Suppress("UNCHECKED_CAST")
private fun <T: Any> T.toValuesSkipping(skipNames: Set<String>): Map<String, Any?> =
  toValues((this::class.memberProperties as Iterable<KProperty1<T, *>>).filter { !skipNames.contains(it.name) })

fun <T: Any> T.toValues(props: Iterable<KProperty1<T, *>>): Map<String, Any?> =
  props.filter { it.visibility == PUBLIC && it.javaField != null }.associate { it.name to persistEmptyCollectionType(it.get(this), it.returnType) }

private fun persistEmptyCollectionType(v: Any?, type: KType) =
  if (v is Collection<*> && v.isEmpty()) EmptyOf((type.arguments.first().type!!.classifier as KClass<*>).javaObjectType) else v

data class EmptyOf<T: Any>(val type: Class<T>): Collection<T> by emptyList()

inline fun <reified T: Any> ResultSet.fromValues(vararg provided: PropValue<T, *>) = fromValues(T::class, *provided)
inline fun <reified T: Any> Map<String, Any?>.fromValues() = fromValues(T::class)

fun <T: Any> ResultSet.fromValues(type: KClass<T>, vararg provided: PropValue<T, *>): T {
  val extraArgs = provided.associate { it.property.name to it.value }
  return extraArgs.fromValues(type) {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name!!]
    else if (it.isOptional) getOptional(it.name!!, it.type)
    else get(it.name!!, it.type)
  }
}

fun <T: Any> Map<String, Any?>.fromValues(type: KClass<T>, getValue: (KParameter) -> Any? = { get(it.name) }): T {
  val constructor = type.primaryConstructor!!
  val args = constructor.parameters.associateWith { getValue(it) }.filterNot { it.key.isOptional && it.value == null }
  return try {
    constructor.callBy(args)
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException("Cannot create $type using " + args.mapKeys { it.key.name }, e)
  }
}

data class PropValue<T, out V>(val property: KProperty1<T, V>, val value: V)
infix fun <T, V> KProperty1<T, V>.to(value: V) = PropValue(this, value)

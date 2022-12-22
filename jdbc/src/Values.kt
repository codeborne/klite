package klite.jdbc

import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

fun <T: Any> T.toValues(vararg provided: Pair<KProperty1<T, *>, Any?>): Map<String, Any?> {
  val values = mapOf(*provided).mapKeys { it.key.name }
  return toValuesSkipping(values.keys) + values
}

fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(skip.map { it.name }.toSet())

@Suppress("UNCHECKED_CAST")
private fun <T: Any> T.toValuesSkipping(skipNames: Set<String>): Map<String, Any?> =
  toValues((this::class.memberProperties as Iterable<KProperty1<T, *>>).filter { !skipNames.contains(it.name) })

fun <T: Any> T.toValues(props: Iterable<KProperty1<T, *>>): Map<String, Any?> =
  props.filter { it.javaField != null }.associate { it.name to persistEmptyCollectionType(it.get(this), it.returnType) }

private fun persistEmptyCollectionType(v: Any?, type: KType) =
  if (v is Collection<*> && v.isEmpty()) java.lang.reflect.Array.newInstance((type.arguments.first().type!!.classifier as KClass<*>).java, 0) else v

inline fun <reified T: Any> ResultSet.fromValues(vararg provided: Pair<KProperty1<T, *>, Any?>) = fromValues(T::class, *provided)

fun <T: Any> ResultSet.fromValues(type: KClass<T>, vararg provided: Pair<KProperty1<T, *>, Any?>) = type.primaryConstructor!!.let { constructor ->
  val extraArgs = provided.associate { it.first.name to it.second }
  val args = constructor.parameters.associateWith {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name] else get(it.name!!, it.type)
  }
  try { constructor.callBy(args)}
  catch (e: IllegalArgumentException) {
    throw IllegalArgumentException("Cannot create $type using " + args.mapKeys { it.key.name })
  }
}

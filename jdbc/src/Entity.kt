package klite.jdbc

import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

interface IdEntity<ID> {
  val id: ID
}

interface Entity: IdEntity<UUID>

fun <T: Any> T.toValues(vararg provided: Pair<KProperty1<T, *>, Any?>): Map<String, Any?> {
  val values = mapOf(*provided).mapKeys { it.key.name }
  return toValuesSkipping(values.keys) + values
}

fun <T: Any> T.toValuesSkipping(vararg skip: KProperty1<T, *>) = toValuesSkipping(skip.map { it.name }.toSet())

@Suppress("UNCHECKED_CAST")
private fun <T: Any> T.toValuesSkipping(skipNames: Set<String>): Map<String, Any?> =
  toValues((this::class.memberProperties as Iterable<KProperty1<T, *>>).filter { !skipNames.contains(it.name) })

fun <T: Any> T.toValues(props: Iterable<KProperty1<T, *>>): Map<String, Any?> {
  if (this is Persistable<*> && !hasId()) setId()
  return props.filter { it.javaField != null }.associate { it.name to it.get(this) }
}

inline fun <reified T: Any> ResultSet.fromValues(vararg provided: Pair<KProperty1<T, *>, Any?>) = fromValues(T::class, *provided)

fun <T: Any> ResultSet.fromValues(type: KClass<T>, vararg provided: Pair<KProperty1<T, *>, Any?>) = type.primaryConstructor!!.let { constructor ->
  val extraArgs = provided.associate { it.first.name to it.second }
  val args = constructor.parameters.associateWith {
    if (extraArgs.containsKey(it.name)) extraArgs[it.name] else JdbcConverter.from(getObject(it.name), it.type)
  }
  constructor.callBy(args).apply { if (this is Persistable<*>) setId(extraArgs["id"] as UUID? ?: getId()) }
}

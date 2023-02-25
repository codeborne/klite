package klite

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
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

private val publicPropsCache = ConcurrentHashMap<KClass<*>, Sequence<KProperty1<*, *>>>()

@Suppress("UNCHECKED_CAST")
val <T: Any> KClass<T>.publicProperties get() = publicPropsCache.getOrPut(this) {
  memberProperties.filter { it.visibility == PUBLIC }.asSequence()
} as Sequence<KProperty1<T, *>>

@Suppress("UNCHECKED_CAST")
val <T: Any> T.publicProperties get() = this::class.publicProperties as Sequence<KProperty1<T, *>>

private fun <T: Any> T.toValuesSkipping(skipNames: Set<String>): Map<String, Any?> = toValues(publicProperties.filter { it.name !in skipNames })

fun <T> KProperty1<T, *>.valueOf(o: T) = try { get(o) } catch (e: InvocationTargetException) { throw e.targetException }

fun <T: Any> T.toValues(props: Sequence<KProperty1<T, *>>): Map<String, Any?> =
  props.filter { it.javaField != null }.associate { it.name to it.valueOf(this) }

fun <T: Any> KClass<T>.create(valueOf: (KParameter) -> Any?): T {
  val constructor = primaryConstructor ?: error("$this does not have primary constructor")
  val args = constructor.parameters.associateWith { valueOf(it) }.filterNot { it.key.isOptional && it.value == null }
  return try {
    constructor.callBy(args)
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException("Cannot create $this using " + args.mapKeys { it.key.name }, e)
  }
}

fun KType.createFrom(values: Map<String, Any?>) = (classifier as KClass<*>).createFrom(values)
fun <T: Any> KClass<T>.createFrom(values: Map<String, Any?>) = create { Converter.from(values[it.name], it.type) }

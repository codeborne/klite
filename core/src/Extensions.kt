package klite

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun String?.trimToNull() = this?.trim()?.takeIf { it.isNotEmpty() }

@Suppress("UNCHECKED_CAST")
fun <K, V> notNullValues(vararg pairs: Pair<K, V?>?) = pairs.filter { it?.second != null } as List<Pair<K, V>>
fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V?>?) = notNullValues(*pairs).toMap()

val KType.java get() = (classifier as KClass<*>).java
fun Any.unboxInline() = javaClass.getMethod("unbox-impl").invoke(this)

val String.uuid: UUID get() = UUID.fromString(this)

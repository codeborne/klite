package klite

import kotlin.reflect.KClass
import kotlin.reflect.KType

fun String?.trimToNull() = this?.trim()?.takeIf { it.isNotEmpty() }

@Suppress("UNCHECKED_CAST")
fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V?>) = mapOf(*pairs).filterValues { it != null } as Map<K, V>

val KType.java get() = (classifier as KClass<*>).java

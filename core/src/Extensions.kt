package klite

import kotlin.reflect.KClass
import kotlin.reflect.KType

fun String?.trimToNull() = this?.trim()?.takeIf { it.isNotEmpty() }

@Suppress("UNCHECKED_CAST")
fun <V> mapOfNotNull(vararg pairs: Pair<String, V?>) = mapOf(*pairs).filterValues { it != null } as Map<String, V>

val KType.java get() = (classifier as KClass<*>).java

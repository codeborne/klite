package klite

fun String?.trimToNull() = this?.trim()?.takeIf { it.isNotEmpty() }

@Suppress("UNCHECKED_CAST")
fun <V> mapOfNotNull(vararg pairs: Pair<String, V?>) = mapOf(*pairs).filterValues { it != null } as Map<String, V>

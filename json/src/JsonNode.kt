@file:Suppress("UNCHECKED_CAST")
package klite.json

typealias JsonNode = Map<String, Any?>

fun JsonNode.getString(key: String) = get(key) as String
fun JsonNode.getInt(key: String) = (get(key) as Number).toInt()
fun JsonNode.getLong(key: String) = (get(key) as Number).toLong()
fun JsonNode.getBigDecimal(key: String) = (get(key) as Number).toDouble().toBigDecimal()
fun <T> JsonNode.getList(key: String) = get(key) as List<T>
fun <T> JsonNode.getMap(key: String) = get(key) as Map<String, T>
fun JsonNode.getNode(key: String): JsonNode = getMap(key)

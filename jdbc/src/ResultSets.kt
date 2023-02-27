package klite.jdbc

import klite.uuid
import java.sql.ResultSet
import java.time.Period
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
fun <T> ResultSet.get(column: String, type: KType): T = JdbcConverter.from(getObject(column), type) as T
inline operator fun <reified T> ResultSet.get(column: String): T = get(column, typeOf<T>())

fun <T> ResultSet.getOptional(column: String, type: KType): T? = runCatching { get<T>(column, type) }.getOrNull()
inline fun <reified T> ResultSet.getOptional(column: String): T? = getOptional(column, typeOf<T>())

fun ResultSet.getUuid(column: String = "id") = getString(column).uuid
fun ResultSet.getUuidOrNull(column: String = "id") = getString(column)?.uuid

fun ResultSet.getInstant(column: String) = getTimestamp(column).toInstant()
fun ResultSet.getInstantOrNull(column: String) = getTimestamp(column)?.toInstant()
fun ResultSet.getLocalDate(column: String) = getDate(column).toLocalDate()
fun ResultSet.getLocalDateOrNull(column: String) = getDate(column)?.toLocalDate()
fun ResultSet.getPeriod(column: String) = Period.parse(getString(column))
fun ResultSet.getPeriodOrNull(column: String) = getString(column)?.let { Period.parse(it) }

fun ResultSet.getIntOrNull(column: String) = getInt(column).takeUnless { wasNull() }
fun ResultSet.getLongOrNull(column: String) = getLong(column).takeUnless { wasNull() }
fun ResultSet.getFloatOrNull(column: String) = getFloat(column).takeUnless { wasNull() }
fun ResultSet.getDoubleOrNull(column: String) = getDouble(column).takeUnless { wasNull() }

inline fun <reified T: Enum<T>> ResultSet.getEnum(column: String) = enumValueOf<T>(getString(column))
inline fun <reified T: Enum<T>> ResultSet.getEnumOrNull(column: String) = getString(column)?.let { enumValueOf<T>(it) }

package klite.jdbc

import klite.Converter
import klite.annotations.annotation
import org.intellij.lang.annotations.Language
import java.lang.reflect.Modifier.isStatic
import java.net.URI
import java.net.URL
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Instant
import java.time.Period
import java.time.ZoneOffset.UTC
import java.util.*
import javax.sql.DataSource

fun <R> DataSource.query(table: String, id: UUID, mapper: ResultSet.() -> R): R =
  query(table, mapOf("id" to id), mapper = mapper).firstOrNull() ?: throw NoSuchElementException("$table:$id not found")

fun <R> DataSource.query(table: String, where: Map<String, Any?>, suffix: String = "", mapper: ResultSet.() -> R): List<R> =
  select("select * from $table", where, suffix, mapper)

fun <R> DataSource.select(@Language("SQL") select: String, where: Map<String, Any?>, suffix: String = "", mapper: ResultSet.() -> R): List<R> = withConnection {
  prepareStatement("$select${whereExpr(where)} $suffix").use { stmt ->
    stmt.setAll(whereValues(where))
    stmt.executeQuery().map(mapper)
  }
}

fun DataSource.exec(@Language("SQL") expr: String, values: Sequence<Any?> = emptySequence()): Int = withConnection {
  prepareStatement(expr).use { stmt ->
    stmt.setAll(values)
    stmt.executeUpdate()
  }
}

fun DataSource.insert(table: String, values: Map<String, *>): Int =
  exec(insertExpr(table, values), setValues(values))

fun DataSource.upsert(table: String, values: Map<String, *>, uniqueFields: String = "id"): Int =
  exec(insertExpr(table, values) + " on conflict ($uniqueFields) do update set ${setExpr(values)}", setValues(values) + setValues(values))

private fun insertExpr(table: String, values: Map<String, *>) = """
  insert into $table (${values.keys.joinToString()})
    values (${values.entries.joinToString { (it.value as? SqlExpr)?.expr(it.key) ?: "?" }})""".trimIndent()

fun DataSource.update(table: String, where: Map<String, Any?>, values: Map<String, *>): Int =
  exec("update $table set ${setExpr(values)}${whereExpr(where)}", setValues(values) + whereValues(where))

fun DataSource.delete(table: String, where: Map<String, Any?>): Int =
  exec("delete from $table${whereExpr(where)}", whereValues(where))

private fun setExpr(values: Map<String, *>) = values.entries.joinToString { it.key + " = " + ((it.value as? SqlExpr)?.expr(it.key) ?: "?") }

private fun whereExpr(where: Map<String, Any?>) = if (where.isEmpty()) "" else " where " +
  where.entries.joinToString(" and ") { (k, v) -> whereExpr(k, v) }

private fun whereExpr(k: String, v: Any?) = when(v) {
  null -> "$k is null"
  is SqlExpr -> v.expr(k)
  is Iterable<*> -> inExpr(k, v)
  is Array<*> -> inExpr(k, v.toList())
  else -> "$k = ?"
}

private fun inExpr(k: String, v: Iterable<*>) = "$k in (${v.joinToString { "?" }})"

private fun setValues(values: Map<String, Any?>) = values.values.asSequence().flatMap { it.flatExpr() }
private fun Any?.flatExpr(): Iterable<Any?> = if (this is SqlExpr) values else listOf(this)

private fun whereValues(where: Map<String, Any?>) = where.values.asSequence().filterNotNull().flatMap { it.toIterable() }
private fun Any?.toIterable(): Iterable<Any?> = when (this) {
  is Array<*> -> toList()
  is Iterable<*> -> this
  else -> flatExpr()
}

operator fun PreparedStatement.set(i: Int, value: Any?) = setObject(i, connection.toDBType(value))
fun PreparedStatement.setAll(values: Sequence<Any?>) = values.forEachIndexed { i, v -> this[i + 1] = v }

private fun Connection.toDBType(v: Any?): Any? = when(v) {
  null -> null
  is Enum<*> -> v.name
  is Instant -> v.atOffset(UTC)
  is Collection<*> -> createArrayOf(if (v.firstOrNull() is UUID) "uuid" else "varchar", v.toTypedArray())
  is Period, is URL, is URI, is Currency, is Locale -> v.toString()
  is UUID -> v
  else -> when {
    v::class.annotation<JvmInline>() != null -> v.javaClass.declaredFields.first { !isStatic(it.modifiers) }.apply { isAccessible = true }.get(v)
    Converter.supports(v::class) -> v.toString()
    else -> v
  }
}

private fun <R> ResultSet.map(mapper: ResultSet.() -> R): List<R> = mutableListOf<R>().also {
  while (next()) it += mapper()
}

fun ResultSet.getId(column: String = "id") = getString(column).toId()
fun ResultSet.getIdOrNull(column: String) = getString(column)?.toId()
fun ResultSet.getInstant(column: String) = getTimestamp(column).toInstant()
fun ResultSet.getLocalDate(column: String) = getDate(column).toLocalDate()
fun ResultSet.getPeriod(column: String) = Period.parse(getString(column))
fun ResultSet.getIntOrNull(column: String) = getObject(column)?.let { (it as Number).toInt() }

fun String.toId(): UUID = UUID.fromString(this)

inline fun <reified T: Enum<T>> ResultSet.getEnum(column: String) = enumValueOf<T>(getString(column))
inline fun <reified T: Enum<T>> ResultSet.getEnumOrNull(column: String) = getString(column)?.let { enumValueOf<T>(it) }

open class SqlExpr(@Language("SQL") protected val expr: String, val values: Collection<*> = emptyList<Any>()) {
  constructor(expr: String, vararg values: Any?): this(expr, values.toList())
  open fun expr(key: String) = expr
}

class SqlComputed(@Language("SQL") expr: String): SqlExpr(expr) {
  override fun expr(key: String) = "$key = $expr"
}

open class SqlOp(val operator: String, value: Any? = null): SqlExpr(operator, if (value != null) listOf(value) else emptyList()) {
  override fun expr(key: String) = "$key $operator" + ("?".takeIf { values.isNotEmpty() } ?: "")
}

val notNull = SqlOp("is not null")

class Between(from: Any, to: Any): SqlExpr("", from, to) {
  override fun expr(key: String) = "$key between ? and ?"
}

class BetweenExcl(from: Any, to: Any): SqlExpr("", from, to) {
  override fun expr(key: String) = "$key >= ? and $key < ?"
}

class NullOrOp(operator: String, value: Any?): SqlOp(operator, value) {
  override fun expr(key: String) = "($key is null or $key $operator ?)"
}

class NotIn(values: Collection<*>): SqlExpr("", values) {
  constructor(vararg values: Any?): this(values.toList())
  override fun expr(key: String) = inExpr(key, values).replace(" in ", " not in ")
}

@file:Suppress("NAME_SHADOWING")
package klite.jdbc

import org.intellij.lang.annotations.Language
import java.io.InputStream
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Statement.NO_GENERATED_KEYS
import java.sql.Statement.RETURN_GENERATED_KEYS
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KProperty1

val namesToQuote = mutableSetOf("limit", "offset", "check", "table", "column", "constraint", "default", "desc", "distinct", "end", "foreign", "from", "grant", "group", "primary", "user")

typealias Mapper<R> = ResultSet.() -> R
internal typealias Column = Any // String | KProperty1
typealias Where = Map<out Column, Any?>
typealias Values = Map<out Column, *>

// TODO: maybe replace query<>select

fun <R, ID> DataSource.query(table: String, id: ID, mapper: Mapper<R>): R =
  query(table, mapOf("id" to id), into = ArrayList(1), mapper = mapper).firstOrNull() ?: throw NoSuchElementException("$table:$id not found")

fun <R, C: MutableCollection<R>> DataSource.query(table: String, where: Where = emptyMap(), suffix: String = "", into: C, mapper: Mapper<R>): C =
  select("select * from " + q(table), where, suffix, into, mapper)

fun <R> DataSource.query(table: String, where: Where = emptyMap(), suffix: String = "", mapper: Mapper<R>) =
  query(table, where, suffix, mutableListOf(), mapper) as List<R>

inline fun <reified R> DataSource.query(table: String, where: Where = emptyMap(), suffix: String = ""): List<R> =
  query(table, where, suffix) { fromValues() }

fun <R, C: MutableCollection<R>> DataSource.select(@Language("SQL") select: String, where: Where = emptyMap(), suffix: String = "", into: C, mapper: Mapper<R>): C =
  whereConvert(where).let { where ->
  withStatement("$select${whereExpr(where)} $suffix") {
    setAll(whereValues(where))
    into.also { executeQuery().process(it::add, mapper) }
  }
}

@Suppress("UNCHECKED_CAST")
fun <R> DataSource.select(@Language("SQL") select: String, where: Where = emptyMap(), suffix: String = "", mapper: Mapper<R>) =
  select(select, where, suffix, mutableListOf(), mapper) as List<R>

inline fun <reified R> DataSource.select(@Language("SQL") select: String, where: Where = emptyMap(), suffix: String = ""): List<R> =
  select(select, where, suffix) { fromValues() }

internal inline fun <R> ResultSet.process(consumer: (R) -> Unit = {}, mapper: Mapper<R>) {
  while (next()) consumer(mapper())
}

fun DataSource.exec(@Language("SQL") expr: String, values: Sequence<Any?> = emptySequence(), keys: Int = NO_GENERATED_KEYS, callback: (Statement.() -> Unit)? = null): Int =
  withStatement(expr, keys) {
    setAll(values)
    executeUpdate().also {
      if (callback != null) callback()
    }
  }

fun <R> DataSource.withStatement(@Language("SQL") sql: String, keys: Int = NO_GENERATED_KEYS, block: PreparedStatement.() -> R): R = withConnection {
  try {
    prepareStatement(sql, keys).use { it.block() }
  } catch (e: SQLException) {
    throw if (e.message?.contains("unique constraint") == true) AlreadyExistsException(e)
          else SQLException(e.message + "\nSQL: $sql", e.sqlState, e.errorCode, e)
  }
}

// TODO: add insert with mapper that returns the generated keys
fun DataSource.insert(table: String, values: Values): Int {
  val valuesToSet = values.filter { it.value !is GeneratedKey<*> }
  val hasGeneratedKeys = valuesToSet.size != values.size
  return exec(insertExpr(table, valuesToSet), setValues(valuesToSet), if (hasGeneratedKeys) RETURN_GENERATED_KEYS else NO_GENERATED_KEYS) {
    if (hasGeneratedKeys) processGeneratedKeys(values)
  }
}

fun DataSource.upsert(table: String, values: Values, uniqueFields: String = "id"): Int =
  exec(insertExpr(table, values) + " on conflict ($uniqueFields) do update set ${setExpr(values)}", setValues(values) + setValues(values))

internal fun insertExpr(table: String, values: Values) =
  "insert into ${q(table)} (${values.keys.joinToString { q(name(it)) }}) values (${insertValues(values.values)})"

private fun insertValues(values: Iterable<*>) = values.joinToString { v ->
  if (v is SqlExpr) v.expr
  else if (isEmptyCollection(v)) emptyArray.expr
  else "?"
}

fun DataSource.update(table: String, where: Where, values: Values): Int = whereConvert(where).let { where ->
  exec("update ${q(table)} set ${setExpr(values)}${whereExpr(where)}", setValues(values) + whereValues(where))
}

fun DataSource.delete(table: String, where: Where): Int = whereConvert(where).let { where ->
  exec("delete from ${q(table)}${whereExpr(where)}", whereValues(where))
}

internal fun setExpr(values: Values) = values.entries.joinToString { (k, v) ->
  val n = name(k)
  if (v is SqlExpr) v.expr(n)
  else if (isEmptyCollection(v)) emptyArray.expr(n)
  else q(n) + "=?"
}

private fun isEmptyCollection(v: Any?) = v is Collection<*> && v.isEmpty() || v is Array<*> && v.isEmpty()

internal fun whereConvert(where: Where) = where.mapValues { (_, v) ->
  if (isEmptyCollection(v)) emptyArray
  else when (v) {
    null -> isNull
    is Iterable<*> -> In(v)
    is Array<*> -> In(*v)
    is ClosedRange<*> -> Between(v)
    is OpenEndRange<*> -> BetweenExcl(v)
    else -> v
  }
}

internal fun whereExpr(where: Where) = if (where.isEmpty()) "" else " where " + where.join(" and ")

internal fun Where.join(separator: String) = entries.joinToString(separator) { (k, v) ->
  val n = name(k)
  if (v is SqlExpr) v.expr(n) else q(n) + "=?"
}

internal fun name(key: Any) = when(key) {
  is KProperty1<*, *> -> key.name
  is String -> key
  else -> throw UnsupportedOperationException("$key should be a KProperty1 or String")
}

internal fun q(name: String) = if (name in namesToQuote) "\"$name\"" else name

internal fun setValues(values: Values) = values.values.asSequence().flatMap { it.toIterable() }

internal fun whereValues(where: Values) = where.values.asSequence().filterNotNull().flatMap { it.toIterable() }
private fun Any?.toIterable(): Iterable<Any?> = if (isEmptyCollection(this)) emptyList() else if (this is SqlExpr) values else listOf(this)

operator fun PreparedStatement.set(i: Int, value: Any?) {
  if (value is InputStream) setBinaryStream(i, value)
  else setObject(i, JdbcConverter.to(value, connection))
}
fun PreparedStatement.setAll(values: Sequence<Any?>) = values.forEachIndexed { i, v -> this[i + 1] = v }

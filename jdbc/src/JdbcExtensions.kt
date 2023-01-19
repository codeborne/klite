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

fun <R, ID> DataSource.query(table: String, id: ID, mapper: Mapper<R>): R =
  query(table, mapOf("id" to id), into = ArrayList(1), mapper = mapper).firstOrNull() ?: throw NoSuchElementException("$table:$id not found")

fun <R> DataSource.query(table: String, where: Where = emptyMap(), suffix: String = "", into: MutableCollection<R> = mutableListOf(), mapper: Mapper<R>): Collection<R> =
  select("select * from $table", where, suffix, into, mapper)

// backwards-compatibility
fun <R> DataSource.query(table: String, where: Where = emptyMap(), suffix: String = "", mapper: Mapper<R>) =
  query(table, where, suffix, mutableListOf(), mapper) as List<R>

inline fun <reified R> DataSource.query(table: String, where: Where = emptyMap(), suffix: String = ""): List<R> =
  query(table, where, suffix) { fromValues() }

fun <R> DataSource.select(@Language("SQL") select: String, where: Where = emptyMap(), suffix: String = "", into: MutableCollection<R>, mapper: Mapper<R>): MutableCollection<R> =
  withStatement("$select${where.expr} $suffix") {
    setAll(whereValues(where))
    into.also { executeQuery().process(it::add, mapper) }
  }

@Suppress("UNCHECKED_CAST") // backwards-compatibility
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
fun DataSource.insert(table: String, values: Map<String, *>): Int {
  val valuesToSet = values.filter { it.value !is GeneratedKey<*> }
  val hasGeneratedKeys = valuesToSet.size != values.size
  return exec(insertExpr(table, valuesToSet), setValues(valuesToSet), if (hasGeneratedKeys) RETURN_GENERATED_KEYS else NO_GENERATED_KEYS) {
    if (hasGeneratedKeys) processGeneratedKeys(values)
  }
}

fun DataSource.upsert(table: String, values: Map<String, *>, uniqueFields: String = "id"): Int =
  exec(insertExpr(table, values) + " on conflict ($uniqueFields) do update set ${setExpr(values)}", setValues(values) + setValues(values))

private fun insertExpr(table: String, values: Map<String, *>) = """
  insert into $table (${values.keys.joinToString { q(it) }})
    values (${values.entries.joinToString { (it.value as? SqlExpr)?.expr(it.key) ?: "?" }})""".trimIndent()

fun DataSource.update(table: String, where: Where, values: Map<String, *>): Int =
  exec("update $table set ${setExpr(values)}${where.expr}", setValues(values) + whereValues(where))

fun DataSource.delete(table: String, where: Where): Int =
  exec("delete from $table${where.expr}", whereValues(where))

private fun setExpr(values: Map<String, *>) = values.entries.joinToString { q(it.key) + " = " + ((it.value as? SqlExpr)?.expr(it.key) ?: "?") }

private val Where.expr get() = if (isEmpty()) "" else " where " + join(" and ")

internal fun Where.join(separator: String) = entries.joinToString(separator) { (k, v) ->
  val n = name(k)
  when (v) {
    null -> q(n) + " is null"
    is SqlExpr -> v.expr(n)
    is Iterable<*> -> inExpr(n, v)
    is Array<*> -> inExpr(n, v.toList())
    is ClosedRange<*> -> Between(v).expr(n)
    is OpenEndRange<*> -> BetweenExcl(v).expr(n)
    else -> q(n) + " = ?"
  }
}

private fun name(key: Any) = when(key) {
  is KProperty1<*, *> -> key.name
  is String -> key
  else -> throw UnsupportedOperationException("$key should be a KProperty1 or String")
}

internal fun q(name: String) = if (name in namesToQuote) "\"$name\"" else name

internal fun inExpr(k: String, v: Iterable<*>) = q(k) + " in (${v.joinToString { "?" }})"

private fun setValues(values: Map<*, Any?>) = values.values.asSequence().flatMap { it.flatExpr() }
private fun Any?.flatExpr(): Iterable<Any?> = if (this is SqlExpr) values else listOf(this)

internal fun whereValues(where: Map<*, Any?>) = where.values.asSequence().filterNotNull().flatMap { it.toIterable() }
private fun Any?.toIterable(): Iterable<Any?> = when (this) {
  is Array<*> -> toList()
  is Iterable<*> -> this
  // TODO: convert where to avoid duplication here and it Where.expr
  is ClosedRange<*> -> Between(this).values
  is OpenEndRange<*> -> BetweenExcl(this).values
  else -> flatExpr()
}

operator fun PreparedStatement.set(i: Int, value: Any?) {
  if (value is InputStream) setBinaryStream(i, value)
  else setObject(i, JdbcConverter.to(value, connection))
}
fun PreparedStatement.setAll(values: Sequence<Any?>) = values.forEachIndexed { i, v -> this[i + 1] = v }

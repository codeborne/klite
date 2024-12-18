@file:Suppress("NAME_SHADOWING", "NOTHING_TO_INLINE")
package klite.jdbc

import klite.Decimal
import org.intellij.lang.annotations.Language
import java.io.InputStream
import java.sql.*
import java.sql.Statement.NO_GENERATED_KEYS
import java.sql.Statement.RETURN_GENERATED_KEYS
import javax.sql.DataSource
import kotlin.reflect.KProperty1

val namesToQuote = mutableSetOf("limit", "offset", "check", "table", "column", "constraint", "default", "desc", "distinct", "end", "foreign", "from", "grant", "group", "primary", "user")

internal const val selectFrom = "select * from "
internal const val selectFromTable = "$selectFrom table "
internal const val selectWhere = "$selectFromTable where "

typealias Mapper<R> = ResultSet.() -> R
typealias Column = Any // String | KProperty1
typealias ColValue = Pair<Column, Any?>

typealias Where = Collection<ColValue>
typealias ValueMap = Map<out Column, *>

@Deprecated(replaceWith = ReplaceWith("ValueMap"), message = "Use ValueMap instead")
typealias Values = ValueMap

// TODO: return streaming sequences instead of in-memory Lists, be able to convert sequence to json

fun <R, ID> DataSource.select(@Language("SQL", prefix = selectFrom) table: String, id: ID, column: String = "id", suffix: String = "", mapper: Mapper<R>): R =
  select(table, listOf(column to id), suffix, ArrayList(1), mapper).firstOrNull() ?: throw NoSuchElementException("${table.substringBefore(" ")}:$id not found")

fun <R, C: MutableCollection<R>> DataSource.select(@Language("SQL", prefix = selectFrom) table: String, where: Where = emptyList(), @Language("SQL", prefix = selectFromTable) suffix: String = "", into: C, mapper: Mapper<R>): C =
  query(selectFrom + q(table), where, suffix, into, mapper)

inline fun <R> DataSource.select(@Language("SQL", prefix = selectFrom) table: String, vararg where: ColValue?, @Language("SQL", prefix = selectFromTable) suffix: String = "", noinline mapper: Mapper<R>): List<R> =
  select(table, where.filterNotNull(), suffix, mapper = mapper)

fun <R> DataSource.select(@Language("SQL", prefix = selectFrom) table: String, where: Where, @Language("SQL", prefix = selectFromTable) suffix: String = "", mapper: Mapper<R>) =
  select(table, where, suffix, mutableListOf(), mapper) as List<R>

inline fun <reified R> DataSource.select(@Language("SQL", prefix = selectFrom) table: String, where: Where, @Language("SQL", prefix = selectFromTable) suffix: String = ""): List<R> =
  select(table, where, suffix = suffix) { create() }

inline fun <reified R> DataSource.select(@Language("SQL", prefix = selectFrom) table: String, vararg where: ColValue?, @Language("SQL", prefix = selectFromTable) suffix: String = ""): List<R> =
  select(table, *where, suffix = suffix) { create() }

fun <R, C: MutableCollection<R>> DataSource.query(@Language("SQL") select: String, where: Where = emptyList(), @Language("SQL", prefix = selectFromTable) suffix: String = "", into: C, mapper: Mapper<R>): C =
  whereConvert(where).let { where ->
  withStatement("$select${whereExpr(where)} $suffix") {
    setAll(whereValues(where))
    executeQuery().run {
      populatePgColumnNameIndex(select)
      into.also { process(it::add, mapper) }
    }
  }
}

inline fun <R> DataSource.query(@Language("SQL") select: String, vararg where: ColValue?, @Language("SQL", prefix = selectFromTable) suffix: String = "", noinline mapper: Mapper<R>): List<R> =
  query(select, where.filterNotNull(), suffix, mapper = mapper)

fun <R> DataSource.query(@Language("SQL") select: String, where: Where, @Language("SQL", prefix = selectFromTable) suffix: String = "", mapper: Mapper<R>) =
  query(select, where, suffix, mutableListOf(), mapper) as List<R>

inline fun <reified R> DataSource.query(@Language("SQL") select: String, where: Where, @Language("SQL", prefix = selectFromTable) suffix: String = ""): List<R> =
  query(select, where, suffix = suffix) { create() }

inline fun <reified R> DataSource.query(@Language("SQL") select: String, vararg where: ColValue?, @Language("SQL", prefix = selectFromTable) suffix: String = ""): List<R> =
  query(select, *where, suffix = suffix) { create() }

fun DataSource.count(@Language("SQL", prefix = selectFrom) table: String, where: Where = emptyList()) = query("select count(*) from $table", where) { getLong(1) }.first()

internal inline fun <R> ResultSet.process(consumer: (R) -> Unit = {}, mapper: Mapper<R>) {
  while (next()) consumer(mapper())
}

fun DataSource.exec(@Language("SQL") expr: String, vararg values: Any?): Int = exec(expr, values.asSequence())
fun DataSource.exec(@Language("SQL") expr: String, values: Sequence<Any?> = emptySequence(), keys: Int = NO_GENERATED_KEYS, callback: (Statement.() -> Unit)? = null): Int =
  execBatch(expr, sequenceOf(values), keys, callback).first()

fun DataSource.execBatch(@Language("SQL") expr: String, values: Sequence<Sequence<Any?>>, keys: Int = NO_GENERATED_KEYS, callback: (Statement.() -> Unit)? = null): IntArray {
  val i = values.iterator()
  if (!i.hasNext()) return intArrayOf()
  return withStatement(expr, keys) {
    var count = 0
    while (i.hasNext()) {
      setAll(i.next())
      if (count > 0 || i.hasNext()) addBatch()
      count++
    }
    (if (count == 1) intArrayOf(executeUpdate()) else executeBatch()).also {
      if (callback != null) callback()
    }
  }
}

fun <R> DataSource.withStatement(@Language("SQL") sql: String, keys: Int = NO_GENERATED_KEYS, block: PreparedStatement.() -> R): R = withConnection {
  try {
    prepareStatement(sql, keys).use { it.block() }
  } catch (e: SQLException) {
    throw if (e.message?.contains("unique constraint") == true) AlreadyExistsException(e)
          else SQLException(e.message + "\n  SQL: $sql", e.sqlState, e.errorCode, e)
  }
}

// TODO: add insert with mapper that returns the generated keys
fun DataSource.insert(@Language("SQL", prefix = selectFrom) table: String, values: ValueMap, suffix: String = "") =
  insertBatch(table, sequenceOf(values), suffix).first()

fun DataSource.insertBatch(@Language("SQL", prefix = selectFrom) table: String, values: Sequence<ValueMap>, suffix: String = ""): IntArray {
  val keyValuesToSet = values.map { it.filter { it.value !is GeneratedKey<*> } }
  val valuesToSet = keyValuesToSet.map { setValues(it) }
  val first = keyValuesToSet.firstOrNull() ?: return intArrayOf()
  val hasGeneratedKeys = first.size != values.first().size
  return execBatch(insertExpr(table, first) + suffix, valuesToSet, if (hasGeneratedKeys) RETURN_GENERATED_KEYS else NO_GENERATED_KEYS) {
    if (hasGeneratedKeys) processGeneratedKeys(values)
  }
}

fun DataSource.upsert(@Language("SQL", prefix = selectFrom) table: String, values: ValueMap, uniqueFields: String = "id", where: Where = emptyList(), skipUpdateFields: Set<String> = emptySet()): Int =
  upsertBatch(table, sequenceOf(values), uniqueFields, where, skipUpdateFields).first()

fun DataSource.upsertBatch(@Language("SQL", prefix = selectFrom) table: String, values: Sequence<ValueMap>, uniqueFields: String = "id", where: Where = emptyList(), skipUpdateFields: Set<String> = emptySet()): IntArray {
  val where = whereConvert(where.map { (k, v) -> "$table.${q(name(k))}" to v })
  val first = values.firstOrNull() ?: return intArrayOf()
  val updateExpr = first.keys.let { if (skipUpdateFields.isEmpty()) it else it - skipUpdateFields }
                   .joinToString { k -> q(name(k)).let { "$it=excluded.$it" } }
  val whereValues = whereValues(where)
  val valuesToSet = values.map { setValues(it) + whereValues }
  return execBatch(insertExpr(table, first) + " on conflict ($uniqueFields) do update set ${updateExpr}${whereExpr(where)}", valuesToSet)
}

internal fun insertExpr(@Language("SQL", prefix = selectFrom) table: String, values: ValueMap) =
  "insert into ${q(table)} (${values.keys.joinToString { q(name(it)) }}) values (${values.values.joinToString { placeholder(it) }})"

inline fun DataSource.update(@Language("SQL", prefix = selectFrom) table: String, values: ValueMap, vararg where: ColValue?): Int =
  update(table, values, where.filterNotNull())

fun DataSource.update(@Language("SQL", prefix = selectFrom) table: String, values: ValueMap, where: Where): Int = whereConvert(where).let { where ->
  exec("update ${q(table)} set ${setExpr(values)}${whereExpr(where)}", setValues(values) + whereValues(where))
}

inline fun DataSource.delete(@Language("SQL", prefix = selectFrom) table: String, vararg where: ColValue?): Int = delete(table, where.filterNotNull())

fun DataSource.delete(@Language("SQL", prefix = selectFrom) table: String, where: Where): Int = whereConvert(where).let { where ->
  exec("delete from ${q(table)}${whereExpr(where)}", whereValues(where))
}

private fun isEmptyCollection(v: Any?) = v is Collection<*> && v.isEmpty() || v is Array<*> && v.isEmpty()

internal fun whereConvert(where: Where) = where.map { (k, v) -> k to whereValueConvert(v) }
internal fun whereValueConvert(v: Any?) = if (isEmptyCollection(v)) emptyArray else when (v) {
  null -> isNull
  is Iterable<*> -> In(v)
  is Array<*> -> In(*v)
  is ClosedRange<*> -> Between(v)
  is OpenEndRange<*> -> BetweenExcl(v)
  else -> v
}

internal fun setExpr(values: ValueMap) = values.entries.map { (k, v) -> k to v }.join(", ")
internal fun whereExpr(where: Where) = if (where.isEmpty()) "" else " where " + where.join(" and ")

internal fun Iterable<ColValue>.join(separator: String) = joinToString(separator) { (k, v) ->
  val n = name(k)
  if (v is SqlExpr) v.expr(n) else q(n) + "=" + placeholder(v)
}

internal fun name(key: Any) = when(key) {
  is KProperty1<*, *> -> key.name
  is String -> key
  else -> throw UnsupportedOperationException("$key should be a KProperty1 or String")
}

internal fun q(name: String) = if (name in namesToQuote) "\"$name\"" else name

private fun placeholder(v: Any?) = when {
  v is SqlExpr -> v.expr
  isEmptyCollection(v) -> emptyArray.expr
  v is Decimal -> "?::decimal"
  else -> "?"
}

internal fun setValues(values: ValueMap) = values.values.asSequence().flatMap { it.toIterable() }
internal fun whereValues(where: Where) = where.asSequence().map { it.second }.flatValues()
internal fun Sequence<Any?>.flatValues() = filterNotNull().flatMap { it.toIterable() }
private fun Any?.toIterable(): Iterable<Any?> = if (isEmptyCollection(this)) emptyList() else if (this is SqlExpr) values else listOf(this)

operator fun PreparedStatement.set(i: Int, value: Any?) {
  if (value is InputStream) setBinaryStream(i, value)
  else setObject(i, JdbcConverter.to(value, connection))
}

fun PreparedStatement.setAll(values: Sequence<Any?>) = values.forEachIndexed { i, v -> this[i + 1] = v }

var Connection.applicationName get() = getClientInfo("ApplicationName")
                               set(value) { setClientInfo("ApplicationName", value) }

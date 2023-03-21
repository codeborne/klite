package klite.jdbc

import org.intellij.lang.annotations.Language
import kotlin.reflect.KProperty1

private const val selectPrefix = "select * from table where"

open class SqlExpr(@Language("SQL", prefix = selectPrefix) internal val expr: String, val values: Iterable<*> = emptyList<Any>()) {
  constructor(@Language("SQL", prefix = selectPrefix) expr: String, vararg values: Any?): this(expr, values.toList())
  open fun expr(key: String) = expr
  override fun equals(other: Any?) = other === this || other is SqlExpr && other.expr == this.expr && other.values == this.values
  override fun hashCode() = expr.hashCode() + values.hashCode()
}

class SqlComputed(expr: String, vararg values: Any?): SqlExpr(expr, *values) {
  override fun expr(key: String) = q(key) + "=" + expr
}

open class SqlOp(val operator: String, value: Any? = null): SqlExpr(operator, if (value != null) listOf(value) else emptyList()) {
  override fun expr(key: String) = q(key) + " $operator" + (" ?".takeIf { values.firstOrNull() != null } ?: "")
}

@Deprecated("use or() instead")
class NullOrOp(operator: String, value: Any?): SqlOp(operator, value) {
  override fun expr(key: String) = "(${q(key)} is null or ${q(key)} $operator ?)"
}

val isNull = SqlOp("is null")
val notNull = SqlOp("is not null")
val emptyArray = SqlComputed("'{}'")

fun jsonb(value: String) = SqlComputed("?::jsonb", value)

infix fun String.eq(value: Any) = this to value
infix fun String.neq(value: Any) = this to SqlOp("!=", value)
infix fun String.gt(value: Any) = this to SqlOp(">", value)
infix fun String.gte(value: Any) = this to SqlOp(">=", value)
infix fun String.lt(value: Any) = this to SqlOp("<", value)
infix fun String.lte(value: Any) = this to SqlOp("<=", value)
infix fun String.like(value: String) = this to SqlOp("like", value)
infix fun String.ilike(value: String) = this to SqlOp("ilike", value)
infix fun String.any(value: Any) = this to SqlExpr("?=any($this)", value)

infix fun <T, V> KProperty1<T, V>.eq(value: V) = this to value
infix fun <T, V> KProperty1<T, V>.neq(value: V) = this to SqlOp("!=", value)
infix fun <T, V> KProperty1<T, V>.gt(value: V) = this to SqlOp(">", value)
infix fun <T, V> KProperty1<T, V>.gte(value: V) = this to SqlOp(">=", value)
infix fun <T, V> KProperty1<T, V>.lt(value: V) = this to SqlOp("<", value)
infix fun <T, V> KProperty1<T, V>.lte(value: V) = this to SqlOp("<=", value)
infix fun <T, V> KProperty1<T, V>.like(value: String) = this to SqlOp("like", value)
infix fun <T, V> KProperty1<T, V>.ilike(value: String) = this to SqlOp("ilike", value)
infix fun <T, V> KProperty1<T, V>.any(value: Any) = this to SqlExpr("?=any($name)", value)

class Between(from: Comparable<*>, to: Comparable<*>): SqlExpr("", from, to) {
  constructor(range: ClosedRange<*>): this(range.start, range.endInclusive)
  override fun expr(key: String) = q(key) + " between ? and ?"
}

class BetweenExcl(from: Comparable<*>, to: Comparable<*>): SqlExpr("", from, to) {
  constructor(range: OpenEndRange<*>): this(range.start, range.endExclusive)
  override fun expr(key: String) = "${q(key)} >= ? and ${q(key)} < ?"
}

open class In(values: Iterable<*>): SqlExpr("", values) {
  constructor(vararg values: Any?): this(values.toList())
  override fun expr(key: String) = q(key) + " in (${values.joinToString { "?" }})"
}

class NotIn(values: Iterable<*>): In(values) {
  constructor(vararg values: Any?): this(values.toList())
  override fun expr(key: String) = super.expr(key).replace(" in ", " not in ")
}

fun orExpr(vararg where: Pair<Column, Any?>?): SqlExpr = where.asSequence().filterNotNull().map { (k, v) -> k to whereValueConvert(v) }.let {
  SqlExpr("(" + it.asIterable().join(" or ") + ")", it.map { it.second }.flatValues().toList())
}

fun <K: Column> or(vararg where: Pair<K, Any?>?) = where[0]!!.first to orExpr(*where)

@Suppress("UNCHECKED_CAST")
fun <K: Column> sql(@Language("SQL", prefix = selectPrefix) expr: String, vararg values: Any?): Pair<K, SqlExpr> = (expr as K) to SqlExpr(expr, *values)

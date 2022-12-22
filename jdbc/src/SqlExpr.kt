package klite.jdbc

import org.intellij.lang.annotations.Language
import kotlin.reflect.KProperty1

open class SqlExpr(@Language("SQL") protected val expr: String, val values: Collection<*> = emptyList<Any>()) {
  constructor(@Language("SQL") expr: String, vararg values: Any?): this(expr, values.toList())
  open fun expr(key: String) = expr
}

class SqlComputed(@Language("SQL") expr: String): SqlExpr(expr) {
  override fun expr(key: String) = q(key) + " = " + expr
}

open class SqlOp(val operator: String, value: Any? = null): SqlExpr(operator, if (value != null) listOf(value) else emptyList()) {
  override fun expr(key: String) = q(key) + " $operator " + ("?".takeIf { values.isNotEmpty() } ?: "")
}

val notNull = SqlOp("is not null")

infix fun String.eq(value: Any) = this to value
infix fun String.neq(value: Any) = this to SqlOp("!=", value)
infix fun String.gt(value: Any) = this to SqlOp(">", value)
infix fun String.gte(value: Any) = this to SqlOp(">=", value)
infix fun String.lt(value: Any) = this to SqlOp("<", value)
infix fun String.lte(value: Any) = this to SqlOp("<=", value)
infix fun String.like(value: String) = this to SqlOp("like", value)
infix fun String.ilike(value: String) = this to SqlOp("ilike", value)

infix fun <T, V> KProperty1<T, V>.eq(value: V) = this to value
infix fun <T, V> KProperty1<T, V>.neq(value: V) = this to SqlOp("!=", value)
infix fun <T, V> KProperty1<T, V>.gt(value: V) = this to SqlOp(">", value)
infix fun <T, V> KProperty1<T, V>.gte(value: V) = this to SqlOp(">=", value)
infix fun <T, V> KProperty1<T, V>.lt(value: V) = this to SqlOp("<", value)
infix fun <T, V> KProperty1<T, V>.lte(value: V) = this to SqlOp("<=", value)
infix fun <T, V> KProperty1<T, V>.like(value: String) = this to SqlOp("like", value)
infix fun <T, V> KProperty1<T, V>.ilike(value: String) = this to SqlOp("ilike", value)

class Between(from: Any, to: Any): SqlExpr("", from, to) {
  override fun expr(key: String) = q(key) + " between ? and ?"
}

class BetweenExcl(from: Any, to: Any): SqlExpr("", from, to) {
  override fun expr(key: String) = "${q(key)} >= ? and ${q(key)} < ?"
}

class NullOrOp(operator: String, value: Any?): SqlOp(operator, value) {
  override fun expr(key: String) = "(${q(key)} is null or $key $operator ?)"
}

class NotIn(values: Collection<*>): SqlExpr("", values) {
  constructor(vararg values: Any?): this(values.toList())
  override fun expr(key: String) = inExpr(key, values).replace(" in ", " not in ")
}

fun or(vararg where: Pair<Column, Any?>): SqlExpr =
  where.toMap().let { SqlExpr("(" + it.join(" or ") + ")", whereValues(it).toList()) }

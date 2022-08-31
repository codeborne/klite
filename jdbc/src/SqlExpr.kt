package klite.jdbc

import org.intellij.lang.annotations.Language

open class SqlExpr(@Language("SQL") protected val expr: String, val values: Collection<*> = emptyList<Any>()) {
  constructor(@Language("SQL") expr: String, vararg values: Any?): this(expr, values.toList())
  open fun expr(key: String) = expr
}

class SqlComputed(@Language("SQL") expr: String): SqlExpr(expr) {
  override fun expr(key: String) = q(key) + " = " + expr
}

open class SqlOp(val operator: String, value: Any? = null): SqlExpr(operator, if (value != null) listOf(value) else emptyList()) {
  override fun expr(key: String) = q(key) + " $operator" + ("?".takeIf { values.isNotEmpty() } ?: "")
}

val notNull = SqlOp("is not null")

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

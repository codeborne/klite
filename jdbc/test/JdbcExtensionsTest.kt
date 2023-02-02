package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class JdbcExtensionsTest {
  val values = mapOf(
    "hello" to "world",
    "nullable" to null,
    "array" to listOf(1, 2, 3),
    "emptyArray" to emptyArray<Any>(),
    "date" to SqlComputed("current_date")
  )

  @Test fun insertExpr() {
    expect(insertExpr("table", values)).toEqual(
      "insert into \"table\" (hello, nullable, array, emptyArray, date) values (?, ?, ?, '{}', current_date)")
  }

  @Test fun setExpr() {
    expect(setExpr(values)).toEqual("hello=?, nullable=?, array=?, emptyArray='{}', date=current_date")
  }

  @Test fun whereExpr() {
    val where = values + sql("exists (subselect)") + or("a" to "b", "array" any 123, "something" like "x%", "num" gte 1)
    expect(where.expr).toEqual(" where hello=? and nullable is null and array in (?, ?, ?) and emptyArray='{}'" +
      " and date=current_date and exists (subselect) and (a=? or ?=any(array) or something like ? or num >= ?)")
  }
}

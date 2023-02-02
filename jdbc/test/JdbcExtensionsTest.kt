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
}

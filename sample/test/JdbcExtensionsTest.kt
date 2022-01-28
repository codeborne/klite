package klite.jdbc

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.util.*
import java.util.UUID.randomUUID

open class JdbcExtensionsTest: TempTableDBTest() {
  @Test fun `insert & query`() {
    val id = randomUUID()
    db.insert(table, mapOf("id" to id, "hello" to "Hello", "world" to 42))
    val id2 = randomUUID()
    db.insert(table, mapOf("id" to id2, "hello" to "Hello2"))

    expectThat(db.query(table, id) { getId() }).isEqualTo(id)

    expectThat(db.query(table, mapOf("hello" to "Hello")) { getId() }).contains(id)
    expectThat(db.query(table, mapOf("hello" to "World")) { getId() }).isEmpty()

    expectThat(db.query(table, mapOf("hello" to SqlOp(">", "Hello"))) { getId() }).contains(id2)
    expectThat(db.query(table, mapOf("hello" to listOf("Hello", "Hello2"))) { getId() }).contains(id, id2)
    expectThat(db.query(table, mapOf("hello" to listOf("Hello", "Hello2")), "order by hello desc") { getId() }).containsExactly(id2, id)
    expectThat(db.query(table, mapOf("hello" to listOf("Hello", "Hello2")), "limit 1") { getId() }).containsExactly(id)
    expectThat(db.query(table, mapOf("hello" to NotIn("Hello2"))) { getId() }).containsExactly(id)

    expectThat(db.query(table, emptyMap(), "where world is null") { getId() }).containsExactly(id2)
    expectThat(db.query("$table a join $table b on a.id = b.id", emptyMap()) { getId() }).contains(id, id2)
  }

  @Test fun upsert() {
    val data = SomeData("World", 37)
    db.upsert(table, data.toValues())
    db.upsert(table, data.toValues())

    val loaded: SomeData = db.query(table, data.id) { fromValues() }
    expectThat(loaded).isEqualTo(data)
  }

  @Test fun `update and delete`() {
    val data = SomeData("World", 37)
    db.insert(table, data.toValues())

    db.update(table, mapOf("id" to data.id), mapOf("world" to 39))
    expectThat(db.query(table, data.id) { fromValues<SomeData>() }).isEqualTo(data.copy(world = 39))

    db.delete(table, mapOf("world" to 39))
    expectThrows<NoSuchElementException> { db.query(table, data.id) { } }
  }

  data class SomeData(val hello: String, val world: Int, val id: UUID = randomUUID())
}

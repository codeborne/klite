package klite.jdbc

import net.oddpoet.expect.Expect
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import java.util.UUID.randomUUID

fun <E : Any?, T : Collection<E>> Expect<T>.containOnly(vararg items: E) =
  satisfyThat("contain only <${items.literal}>") {
    it.containsAll(items.toList()) && it.size == items.size
  }

open class JdbcExtensionsTest: TempTableDBTest() {
  @Test fun `insert & query`() {
    val id = randomUUID()
    db.insert(table, mapOf("id" to id, "hello" to "Hello", "world" to 42))
    val id2 = randomUUID()
    db.insert(table, mapOf("id" to id2, "hello" to "Hello2"))

    expect(db.query(table, id) { getId() }).to.equal(id)

    expect(db.query(table, mapOf("hello" to "Hello")) { getId() }).to.contain(id)
    expect(db.query(table, mapOf("hello" to "World")) { getId() }).to.beEmpty()

    expect(db.query(table, mapOf("hello" to SqlOp(">", "Hello"))) { getId() }).to.contain(id2)
    expect(db.query(table, mapOf("hello" to listOf("Hello", "Hello2"))) { getId() }).to.containAllInSameOrder(id, id2)
    expect(db.query(table, mapOf("hello" to listOf("Hello", "Hello2")), "order by hello desc") { getId() }).to.containAllInSameOrder(id2, id)
    expect(db.query(table, mapOf("hello" to listOf("Hello", "Hello2")), "limit 1") { getId() }).to.containOnly(id)
    expect(db.query(table, mapOf("hello" to NotIn("Hello2"))) { getId() }).to.containOnly(id)

    expect(db.query(table, emptyMap(), "where world is null") { getId() }).to.containOnly(id2)
    expect(db.query("$table a join $table b on a.id = b.id", emptyMap()) { getId() }).to.containAll(id, id2)
  }

  @Test fun upsert() {
    val data = SomeData("World", 37)
    db.upsert(table, data.toValues())
    db.upsert(table, data.toValues())

    val loaded: SomeData = db.query(table, data.id) { fromValues() }
    expect(loaded).to.equal(data)
  }

  @Test fun `update and delete`() {
    val data = SomeData("World", 37)
    db.insert(table, data.toValues())

    db.update(table, mapOf("id" to data.id), mapOf("world" to 39))
    expect(db.query(table, data.id) { fromValues<SomeData>() }).to.equal(data.copy(world = 39))

    db.delete(table, mapOf("world" to 39))
    assertThrows<NoSuchElementException> { db.query(table, data.id) { } }
  }

  data class SomeData(val hello: String, val world: Int, val id: UUID = randomUUID())
}

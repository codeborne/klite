package klite.sample

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import klite.jdbc.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.util.*
import java.util.UUID.randomUUID

open class JdbcExtensionsTest: TempTableDBTest() {
  @Test fun `insert & query`() {
    val id = randomUUID()
    db.insert(table, mapOf("id" to id, "hello" to "Hello", "world" to 42))

    val id2 = randomUUID()
    db.insert(table, mapOf("id" to id2, "hello" to "Hello2"))

    expect(db.query(table, id) { getId() }).toEqual(id)

    expect(db.query(table, mapOf("hello" to "Hello")) { getId() }).toContain(id)
    expect(db.query(table, mapOf("hello" to "World")) { getId() }).toBeEmpty()

    expect(db.query(table, mapOf("hello" to SqlOp(">", "Hello"))) { getId() }).toContain(id2)
    expect(db.query(table, mapOf("hello" to listOf("Hello", "Hello2"))) { getId() }).toContain(id, id2)
    expect(db.query(table, mapOf("hello" to listOf("Hello", "Hello2")), "order by hello desc") { getId() }).toContainExactly(id2, id)
    expect(db.query(table, mapOf("hello" to listOf("Hello", "Hello2")), "limit 1") { getId() }).toContainExactly(id)
    expect(db.query(table, mapOf("hello" to NotIn("Hello2"))) { getId() }).toContainExactly(id)

    expect(db.query(table, emptyMap(), "where world is null") { getId() }).toContainExactly(id2)
    expect(db.query("$table a join $table b on a.id = b.id", emptyMap()) { getId() }).toContain(id, id2)
  }

  @Test fun generatedKey() {
    val generatedKey = GeneratedKey<Int>()
    db.insert(table, mapOf("id" to randomUUID(), "hello" to "Hello", "gen" to generatedKey))
    expect(generatedKey.value).toBeGreaterThanOrEqualTo(1)
  }

  @Test fun `generatedKey with type`() {
    val generatedKey = GeneratedKey(BigDecimal::class)
    db.insert(table, mapOf("id" to randomUUID(), "hello" to "BD", "gen" to generatedKey))
    expect(generatedKey.value).toBeGreaterThan(ZERO)
  }

  @Test fun upsert() {
    val data = SomeData("World", 37)
    db.upsert(table, data.toValues())
    db.upsert(table, data.toValues())

    val loaded: SomeData = db.query(table, data.id) { fromValues() }
    expect(loaded).toEqual(data)
  }

  @Test fun `update and delete`() {
    val data = SomeData("World", 37)
    db.insert(table, data.toValues())

    db.update(table, mapOf("id" to data.id), mapOf("world" to 39))
    expect(db.query(table, data.id) { fromValues<SomeData>() }).toEqual(data.copy(world = 39))

    db.delete(table, mapOf("world" to 39))
    expect { db.query(table, data.id) { } }.toThrow<NoSuchElementException>()
  }

  data class SomeData(val hello: String, val world: Int, val id: UUID = randomUUID())
}

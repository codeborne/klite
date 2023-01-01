package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class ValuesTest {
  @Test fun toValues() {
    val data = SomeData("Hello", 123)
    expect(data.toValues()).toEqual(mapOf("hello" to "Hello", "world" to 123, "nullable" to null, "list" to listOf(1, 2)))
    expect(data.toValues(SomeData::world to 124)).toEqual(mapOf("hello" to "Hello", "world" to 124, "nullable" to null, "list" to listOf(1, 2)))
  }

  @Test fun `toValues persists empty collection component type`() {
    val empty = SomeData("", 1, list = emptyList()).toValues()["list"]
    expect(empty).toEqual(EmptyOf(Int::class.javaObjectType))
    expect(empty as Collection<*>).toBeEmpty()
  }

  @Test fun toValuesSkipping() {
    val data = SomeData("Hello", 123)
    expect(data.toValuesSkipping(SomeData::nullable, SomeData::list)).toEqual(mapOf("hello" to "Hello", "world" to 123))
    expect(data.toValuesSkipping(SomeData::hello, SomeData::world, SomeData::nullable, SomeData::list)).toBeEmpty()
  }

  @Test fun fromValues() {
    val rs = mockk<ResultSet> {
      every { getObject("hello") } returns "Hello"
      every { getObject("world") } returns 42
      every { getObject("list") } returns mockk<java.sql.Array> { every { array } returns arrayOf(4, 5)}
    }
    expect(rs.fromValues<SomeData>()).toEqual(SomeData("Hello", 42, list = listOf(4, 5)))
  }

  @Test fun `fromValues with some values provided`() {
    val rs = mockk<ResultSet>()
    expect(rs.fromValues(SomeData::hello to "Hello", SomeData::world to 42, SomeData::nullable to null, SomeData::list to listOf(9))).toEqual(SomeData("Hello", 42, list = listOf(9)))
  }

  data class SomeData(val hello: String, val world: Int, val nullable: String? = null, val list: List<Int> = listOf(1, 2))
}

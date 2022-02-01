package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.sql.ResultSet
import java.util.UUID.randomUUID

class BaseModelTest {
  @Test fun toValues() {
    val data = SomeData("Hello", 123)
    expect(data.toValues()).toEqual(mapOf("hello" to "Hello", "world" to 123))
    expect(data.toValues(SomeData::world to 124)).toEqual(mapOf("hello" to "Hello", "world" to 124))
  }

  @Test fun `toValues for Persistent generates id if not set`() {
    val data = PersistentData("Hello")
    expect(data.toValuesSkipping(PersistentData::id)).toEqual(mapOf("hello" to "Hello"))
    expect(data.toValues()).toEqual(mapOf("hello" to "Hello", "id" to data.id))
  }

  @Test fun toValuesSkipping() {
    val data = SomeData("Hello", 123)
    expect(data.toValuesSkipping(SomeData::hello)).toEqual(mapOf("world" to 123))
    expect(data.toValuesSkipping(SomeData::hello, SomeData::world)).toBeEmpty()
  }

  @Test fun fromValues() {
    val rs = mockk<ResultSet> {
      every { getObject("hello") } returns "Hello"
      every { getObject("world") } returns 42
    }
    expect(rs.fromValues<SomeData>()).toEqual(SomeData("Hello", 42))
  }

  @Test fun `fromValues with some values provided`() {
    val rs = mockk<ResultSet>()
    expect(rs.fromValues(SomeData::hello to "Hello", SomeData::world to 42)).toEqual(SomeData("Hello", 42))
  }

  @Test fun `fromValues for Persistent`() {
    val id = randomUUID()
    val rs = mockk<ResultSet> {
      every { getObject("hello") } returns "x"
      every { getString("id") } returns id.toString()
    }
    val data = rs.fromValues<PersistentData>()
    expect(data).toEqual(PersistentData("x"))
    expect(data.id).toEqual(id)
  }

  data class SomeData(val hello: String, val world: Int)
  data class PersistentData(val hello: String): Persistent<SomeData>()
}

package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class ValuesTest {
  @Test fun `ResultSet fromValues`() {
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

  @Test fun `Map fromValues`() {
    val data = mapOf("hello" to "Hello", "world" to 34).fromValues<SomeData>()
    expect(data).toEqual(SomeData("Hello", 34))
  }

  data class SomeData(val hello: String, val world: Int, val nullable: String? = null, val list: List<Int> = listOf(1, 2))
}

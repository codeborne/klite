package klite.jdbc

import io.mockk.every
import io.mockk.mockk
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class BaseModelTest {
  @Test fun toValues() {
    val data = SomeData("Hello", 123)
    expect(data.toValues()).to.equal(mapOf("hello" to "Hello", "world" to 123))
  }

  @Test fun toValuesSkipping() {
    val data = SomeData("Hello", 123)
    expect(data.toValuesSkipping(SomeData::hello)).to.equal(mapOf("world" to 123))
    expect(data.toValuesSkipping(SomeData::hello, SomeData::world)).to.equal(emptyMap<String, Any>())
  }

  @Test fun fromValues() {
    val rs = mockk<ResultSet> {
      every { getObject("hello") } returns "Hello"
      every { getObject("world") } returns 42
    }
    expect(rs.fromValues<SomeData>()).to.equal(SomeData("Hello", 42))
  }

  @Test fun `fromValues with some values provided`() {
    val rs = mockk<ResultSet>()
    expect(rs.fromValues(SomeData::hello to "Hello", SomeData::world to 42)).to.equal(SomeData("Hello", 42))
  }

  data class SomeData(val hello: String, val world: Int)
}

package klite.jdbc

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class BaseModelTest {
  @Test fun toValues() {
    val data = SomeData("Hello", 123)
    assertThat(data.toValues()).isEqualTo(mapOf("hello" to "Hello", "world" to 123))
  }

  @Test fun toValuesSkipping() {
    val data = SomeData("Hello", 123)
    assertThat(data.toValuesSkipping(SomeData::hello)).isEqualTo(mapOf("world" to 123))
    assertThat(data.toValuesSkipping(SomeData::hello, SomeData::world)).isEqualTo(emptyMap<String, Any>())
  }

  @Test fun fromValues() {
    val rs = mockk<ResultSet> {
      every { getObject("hello") } returns "Hello"
      every { getObject("world") } returns 42
    }
    assertThat(rs.fromValues<SomeData>()).isEqualTo(SomeData("Hello", 42))
  }

  @Test fun `fromValues with some values provided`() {
    val rs = mockk<ResultSet>()
    assertThat(rs.fromValues(SomeData::hello to "Hello", SomeData::world to 42)).isEqualTo(SomeData("Hello", 42))
  }

  data class SomeData(val hello: String, val world: Int)
}

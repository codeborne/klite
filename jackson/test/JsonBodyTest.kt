package klite.json

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonBodyTest {
  @Test fun `can create Kotlin classes`() {
    val someData = JsonBody().parse("""{"hello":"World"}""".byteInputStream(), SomeData::class)
    assertThat(someData).isEqualTo(SomeData("World"))
  }
}

data class SomeData(val hello: String)


package klite.json

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JsonBodyTest {
  @Test fun `can create Kotlin classes`() {
    val someData = JsonBody().parse("""{"hello":"World"}""".byteInputStream(), SomeData::class)
    expectThat(someData).isEqualTo(SomeData("World"))
  }
}

data class SomeData(val hello: String)


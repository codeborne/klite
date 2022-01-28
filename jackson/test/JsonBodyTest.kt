package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class JsonBodyTest {
  @Test fun `can create Kotlin classes`() {
    val someData = JsonBody().parse("""{"hello":"World"}""".byteInputStream(), SomeData::class)
    expect(someData).toEqual(SomeData("World"))
  }
}

data class SomeData(val hello: String)


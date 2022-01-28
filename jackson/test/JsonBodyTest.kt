package klite.json

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test

class JsonBodyTest {
  @Test fun `can create Kotlin classes`() {
    val someData = JsonBody().parse("""{"hello":"World"}""".byteInputStream(), SomeData::class)
    expect(someData).to.equal(SomeData("World"))
  }
}

data class SomeData(val hello: String)


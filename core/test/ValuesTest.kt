package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class ValuesTest {
  @Test fun toValues() {
    val data = SomeData("Hello", 123)
    expect(data.toValues()).toEqual(mapOf("hello" to "Hello", "world" to 123, "nullable" to null, "list" to listOf(1, 2)))
    expect(data.toValues(SomeData::world to 124)).toEqual(mapOf("hello" to "Hello", "world" to 124, "nullable" to null, "list" to listOf(1, 2)))
  }

  @Test fun toValuesSkipping() {
    val data = SomeData("Hello", 123)
    expect(data.toValuesSkipping(SomeData::nullable, SomeData::list)).toEqual(mapOf("hello" to "Hello", "world" to 123))
    expect(data.toValuesSkipping(SomeData::hello, SomeData::world, SomeData::nullable, SomeData::list)).toBeEmpty()
  }

  data class SomeData(val hello: String, val world: Int, val nullable: String? = null, val list: List<Int> = listOf(1, 2))
}

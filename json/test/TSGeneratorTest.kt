package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class TSGeneratorTest {
  val ts = TSGenerator()

  @Test fun enum() {
    expect(ts.render(SomeEnum::class)).toEqual(/* language=TypeScript */ "enum SomeEnum {HELLO = 'HELLO', WORLD = 'WORLD'}")
    expect(ts.render(SomeData.Status::class)).toEqual(/* language=TypeScript */ "enum SomeDataStatus {ACTIVE = 'ACTIVE'}")
  }

  @Test fun `interface`() {
    expect(ts.render(Person::class)).toEqual(
      // language=TypeScript
      "interface Person {hello: SomeEnum; name: string}")

    expect(ts.render(SomeData::class)).toEqual(
      // language=TypeScript
      "interface SomeData {age: number; birthDate?: string; id: string; list: SomeData[]; map: Record<string, SomeData[]>; name: string; other?: SomeData; status: SomeDataStatus; hello: SomeEnum}")
  }
}

interface Person { val name: String; val hello get() = SomeEnum.HELLO }
data class SomeData(override val name: String, val age: Int, val birthDate: String?, val id: UUID, val other: SomeData?, val list: List<SomeData>, val map: Map<LocalDate, Array<SomeData>>, val status: Status = Status.ACTIVE): Person {
  enum class Status { ACTIVE }
}
enum class SomeEnum { HELLO, WORLD }

package klite.json

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KProperty1

class TSGeneratorTest {
  val ts = TSGenerator()

  @Test fun enum() {
    expect(ts.render(SomeEnum::class)).toEqual(/* language=TypeScript */ "enum SomeEnum {HELLO = 'HELLO', WORLD = 'WORLD'}")
    expect(ts.render(SomeData.Status::class)).toEqual(/* language=TypeScript */ "enum SomeDataStatus {ACTIVE = 'ACTIVE'}")
  }

  @Test fun inline() {
    expect(ts.render(MyId::class)).toEqual( /* language=TypeScript */ "type MyId<T> = string")
  }

  @Test fun `interface`() {
    expect(ts.render(NoProps::class)).toEqual(null)

    expect(ts.render(Person::class)).toEqual( // language=TypeScript
      "interface Person {hello: SomeEnum; name: string}")

    expect(ts.render(SomeData::class)).toEqual( // language=TypeScript
      "interface SomeData {age: number; birthDate?: string; bytes: Array<number>; field: keyof Person; id: MyId<SomeData>; " +
        "list: Array<SomeData>; map: Record<string, Array<SomeData>>; name: string; other?: SomeData; status: SomeDataStatus; hello: SomeEnum}")
  }
}

@JvmInline value class MyId<T>(val uuid: UUID = UUID.randomUUID())
enum class SomeEnum { HELLO, WORLD }
interface Person { val name: String; val hello get() = SomeEnum.HELLO }
data class SomeData(override val name: String, val age: Int, val birthDate: String?, val id: MyId<SomeData>, val other: SomeData?,
                    val list: List<SomeData>, val map: Map<LocalDate, Array<SomeData>>, val status: Status = Status.ACTIVE,
                    val field: KProperty1<Person, *>, val bytes: ByteArray): Person {
  enum class Status { ACTIVE }
}
interface NoProps { fun onlyMethods() }

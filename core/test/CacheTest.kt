import ch.tutteli.atrium.api.fluent.en_GB.toBeTheInstance
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.Cache
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class CacheTest {
  val data = LocalDate.now()

  @Test fun `set & get`() { Cache<String, LocalDate>(10.milliseconds).use { cache ->
    expect(cache.isEmpty()).toEqual(true)

    cache["key"] = data
    expect(cache.isEmpty()).toEqual(false)

    expect(cache["key"]).toBeTheInstance(data)
    Thread.sleep(12)
    expect(cache["key"]).toEqual(null)
    Thread.sleep(9)
    expect(cache.isEmpty()).toEqual(true)

    expect(cache.getOrSet("key") { data }).toBeTheInstance(data)
    expect(cache["key"]).toBeTheInstance(data)
  }}

  @Test fun prolongOnAccess() { Cache<String, LocalDate>(10.milliseconds, prolongOnAccess = true).use { cache ->
    cache["key"] = data
    Thread.sleep(9)
    expect(cache["key"]).toBeTheInstance(data)
    Thread.sleep(9)
    expect(cache["key"]).toBeTheInstance(data)
    Thread.sleep(11)
    expect(cache["key"]).toEqual(null)
  }}
}

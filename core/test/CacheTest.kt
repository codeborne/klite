import ch.tutteli.atrium.api.fluent.en_GB.toBeTheInstance
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.Cache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class CacheTest {
  val cache = Cache<String, LocalDate>(10.milliseconds)
  @AfterEach fun close() = cache.close()

  @Test fun `set & get`() {
    val data = LocalDate.now()
    expect(cache.isEmpty()).toEqual(true)

    cache["key"] = data
    expect(cache.isEmpty()).toEqual(false)

    expect(cache["key"]).toBeTheInstance(data)
    Thread.sleep(11)
    expect(cache["key"]).toEqual(null)
    expect(cache.isEmpty()).toEqual(false)
    Thread.sleep(9)
    expect(cache.isEmpty()).toEqual(true)

    expect(cache.getOrSet("key") { data }).toBeTheInstance(data)
    expect(cache["key"]).toBeTheInstance(data)
  }
}

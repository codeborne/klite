import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.TSID
import org.junit.jupiter.api.Test

class TSIDTest {
  @Test fun `max value`() {
    expect(TSID(1234L).toString()).toEqual("ya")
    expect(TSID(Long.MAX_VALUE).toString()).toEqual("1y2p0ij32e8e7")
  }

  @Test fun `no collisions`() {
    val ids = mutableSetOf<TSID>()
    for (i in 1..1000000) ids.add(TSID())
    expect(ids.size).toEqual(1000000)
  }
}

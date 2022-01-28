package klite

import net.oddpoet.expect.Expect
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test
import java.net.URI

fun <K, V> Expect<Map<K, V>>.beEmpty() =
  satisfyThat("be empty") {
    it.isEmpty()
  }

class URITest {
  @Test fun queryParams() {
    expect(URI("/hello").queryParams).to.beEmpty()
    expect(URI("/hello?hello").queryParams).to.equal(mapOf("hello" to null))
    expect(URI("/hello?hello=world").queryParams).to.equal(mapOf("hello" to "world"))
    expect(URI("/hello?p1=1&p2=2").queryParams).to.equal(mapOf("p1" to "1", "p2" to "2"))
    expect(URI("/hello?encoded=Hello%20World").queryParams).to.equal(mapOf("encoded" to "Hello World"))
    expect(URI("/hello?encoded=Hello%26World").queryParams).to.equal(mapOf("encoded" to "Hello&World"))
  }
}

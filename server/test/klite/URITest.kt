package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.net.URI

class URITest {
  @Test fun queryParams() {
    expect(URI("/hello").queryParams).toBeEmpty()
    expect(URI("/hello?hello").queryParams).toEqual(mapOf("hello" to null))
    expect(URI("/hello?hello=world").queryParams).toEqual(mapOf("hello" to "world"))
    expect(URI("/hello?p1=1&p2=2").queryParams).toEqual(mapOf("p1" to "1", "p2" to "2"))
    expect(URI("/hello?encoded=Hello%20World").queryParams).toEqual(mapOf("encoded" to "Hello World"))
    expect(URI("/hello?encoded=Hello%26World").queryParams).toEqual(mapOf("encoded" to "Hello&World"))
  }
}

package klite

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.net.URI

class URITest {
  @Test fun queryParams() {
    expectThat(URI("/hello").queryParams).isEmpty()
    expectThat(URI("/hello?hello").queryParams).isEqualTo(mapOf("hello" to null))
    expectThat(URI("/hello?hello=world").queryParams).isEqualTo(mapOf("hello" to "world"))
    expectThat(URI("/hello?p1=1&p2=2").queryParams).isEqualTo(mapOf("p1" to "1", "p2" to "2"))
    expectThat(URI("/hello?encoded=Hello%20World").queryParams).isEqualTo(mapOf("encoded" to "Hello World"))
    expectThat(URI("/hello?encoded=Hello%26World").queryParams).isEqualTo(mapOf("encoded" to "Hello&World"))
  }
}

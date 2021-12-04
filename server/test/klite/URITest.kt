package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class URITest {
  @Test
  fun queryParams() {
    assertThat(URI("/hello").queryParams).isEmpty()
    assertThat(URI("/hello?hello").queryParams).isEqualTo(mapOf("hello" to null))
    assertThat(URI("/hello?hello=world").queryParams).isEqualTo(mapOf("hello" to "world"))
    assertThat(URI("/hello?p1=1&p2=2").queryParams).isEqualTo(mapOf("p1" to "1", "p2" to "2"))
    assertThat(URI("/hello?encoded=Hello%20World").queryParams).isEqualTo(mapOf("encoded" to "Hello World"))
    assertThat(URI("/hello?encoded=Hello%26World").queryParams).isEqualTo(mapOf("encoded" to "Hello&World"))
  }
}

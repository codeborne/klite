package klite

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class UtilsTest {
  @Test fun urlParams() {
    val params = mapOf("Hello" to "Wörld", "1" to "2", "null" to null)
    expectThat(urlEncodeParams(params)).isEqualTo("Hello=W%C3%B6rld&1=2")
    expectThat(urlDecodeParams("Hello=W%C3%B6rld&1=2")).isEqualTo(params - "null")
  }
}

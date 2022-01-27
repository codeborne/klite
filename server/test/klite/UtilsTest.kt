package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtilsTest {
  @Test fun urlParams() {
    val params = mapOf("Hello" to "Wörld", "1" to "2", "null" to null)
    assertThat(urlEncodeParams(params)).isEqualTo("Hello=W%C3%B6rld&1=2")
    assertThat(urlDecodeParams("Hello=W%C3%B6rld&1=2")).isEqualTo(params - "null")
  }
}

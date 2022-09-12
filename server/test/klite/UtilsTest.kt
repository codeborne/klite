package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.net.URI

class UtilsTest {
  @Test fun urlPlus() {
    expect(URI("https://codeborne.com/#ref") + mapOf("hello" to "world")).toEqual(URI("https://codeborne.com/?hello=world#ref"))
    expect(URI("https://codeborne.com/?x=y#ref") + mapOf("hello" to "world")).toEqual(URI("https://codeborne.com/?x=y&hello=world#ref"))
  }

  @Test fun urlParams() {
    val params = mapOf("Hello" to "Wörld", "1" to "2", "null" to null)
    expect(urlEncodeParams(params)).toEqual("Hello=W%C3%B6rld&1=2")
    expect(urlDecodeParams("Hello=W%C3%B6rld&1=2")).toEqual(params - "null")
  }
}

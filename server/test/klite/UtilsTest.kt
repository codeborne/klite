package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.net.URI

class UtilsTest {
  @Test fun queryParams() {
    expect(URI("/hello").queryParams).toBeEmpty()
    expect(URI("/hello?hello").queryParams).toEqual(mapOf("hello" to null))
    expect(URI("/hello?hello=world").queryParams).toEqual(mapOf("hello" to "world"))
    expect(URI("/hello?p1=1&p2=2").queryParams).toEqual(mapOf("p1" to "1", "p2" to "2"))
    expect(URI("/hello?encoded=Hello%20World").queryParams).toEqual(mapOf("encoded" to "Hello World"))
    expect(URI("/hello?encoded=Hello%26World").queryParams).toEqual(mapOf("encoded" to "Hello&World"))
  }

  @Test fun plus() {
    expect(URI("http://codeborne.com") + mapOf("hello" to "world 123")).toEqual(URI("http://codeborne.com?hello=world+123"))
    expect(URI("http://codeborne.com?world=hello") + mapOf("hello" to "world 123")).toEqual(URI("http://codeborne.com?world=hello&hello=world+123"))
    expect(URI("https://codeborne.com/#ref") + mapOf("hello" to "world")).toEqual(URI("https://codeborne.com/?hello=world#ref"))
    expect(URI("https://codeborne.com/?x=y#ref") + mapOf("hello" to "world")).toEqual(URI("https://codeborne.com/?x=y&hello=world#ref"))
  }

  @Test fun urlParams() {
    val params = mapOf("Hello" to "Wörld", "1" to "2", "null" to null)
    expect(urlEncodeParams(params)).toEqual("Hello=W%C3%B6rld&1=2")
    expect(urlDecodeParams("Hello=W%C3%B6rld&1=2")).toEqual(params - "null")
  }

  @Test fun base64() {
    expect("hellöu".base64Encode()).toEqual("aGVsbMO2dQ==")
    expect("aGVsbMO2dQ==".base64Decode().decodeToString()).toEqual("hellöu")
  }

  @Test fun base64Url() {
    expect("hellöu".base64UrlEncode()).toEqual("aGVsbMO2dQ")
    expect("aGVsbMO2dQ".base64UrlDecode().decodeToString()).toEqual("hellöu")
  }
}

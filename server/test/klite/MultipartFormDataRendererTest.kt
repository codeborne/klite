package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class MultipartFormDataRendererTest {
  @Test fun render() {
    val renderer = MultipartFormDataRenderer("--MyBoundary")
    val output = ByteArrayOutputStream()
    renderer.render(output, mapOf("name1" to "value1", "file2" to FileUpload("a.txt", "text/plain", "Content of a.txt.")))
    expect(output.toString()).toEqual("""
      --MyBoundary
      Content-Disposition: form-data; name="name1"

      value1
      --MyBoundary
      Content-Disposition: form-data; name="file2"; filename="a.txt"
      Content-Type: text/plain; charset=UTF-8

      Content of a.txt.
      --MyBoundary--

    """.trimIndent().replace("\n", "\r\n"))
  }
}

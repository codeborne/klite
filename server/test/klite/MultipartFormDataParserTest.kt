package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class MultipartFormDataParserTest {
  val parser = MultipartFormDataParser()

  @Test fun parse() {
    val body = """
      -----------------------------9051914041544843365972754266
      Content-Disposition: form-data; name="text"

      text default
      -----------------------------9051914041544843365972754266
      Content-Disposition: form-data; name="file1"; filename="a.txt"
      Content-Type: text/plain

      Content of a.txt.

      -----------------------------9051914041544843365972754266
      Content-Disposition: form-data; name="file2"; filename="a.html"
      Content-Type: text/html

      <!DOCTYPE html><title>Content of a.html.</title>

      -----------------------------9051914041544843365972754266
    """.trimIndent()

    expect(parser.parse(body.byteInputStream(), Map::class)).toEqual(mapOf(
      "text" to "text default",
      "file1" to FileUpload("a.txt", "text/plain", "Content of a.txt.\n"),
      "file2" to FileUpload("a.html", "text/html", "<!DOCTYPE html><title>Content of a.html.</title>\n")
    ))
  }
}

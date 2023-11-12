package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

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

      -----------------------------9051914041544843365972754266--
    """.trimIndent()

    val result = parser.parse<Any>(body.byteInputStream(), typeOf<Map<String, Any>>()) as Map<String, Any>
    expect(result["text"]).toEqual("text default")

    val file1 = result["file1"] as FileUpload
    expect(file1.fileName).toEqual("a.txt")
    expect(file1.contentType).toEqual(MimeTypes.text)
    expect(file1.stream.reader().readText()).toEqual("Content of a.txt.\n")

    val file2 = result["file2"] as FileUpload
    expect(file2.fileName).toEqual("a.html")
    expect(file2.contentType).toEqual(MimeTypes.html)
    expect(file2.stream.reader().readText()).toEqual("<!DOCTYPE html><title>Content of a.html.</title>\n")
  }
}

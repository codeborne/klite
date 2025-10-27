package klite

import java.io.OutputStream
import java.io.OutputStreamWriter

typealias FormDataRenderer = MultipartRenderer

class MultipartRenderer(
  override val contentType: String = MimeTypes.formData,
  val boundary: String = "----" + System.currentTimeMillis()
): BodyRenderer {
  val fullContentType = "$contentType; boundary=$boundary"

  override fun render(output: OutputStream, value: Any?) = render(output, value as Map<String, Any?>)

  fun render(output: OutputStream, value: Map<String, Any?>) {
    val w = OutputStreamWriter(output)
    value.forEach { (k, v) ->
      w.write("--")
      w.write(boundary)
      w.write("\r\n")
      w.write("Content-Disposition: form-data; name=\"$k\"")
      when (v) {
        is FileUpload -> {
          w.write("; filename=\"${v.fileName}\"\r\n")
          w.write("Content-Type: ${MimeTypes.withCharset(v.contentType ?: MimeTypes.unknown)}\r\n\r\n")
          w.flush()
          v.stream.use { it.copyTo(output) }
        }
        else -> {
          w.write("\r\n\r\n")
          w.write(v.toString())
        }
      }
      w.write("\r\n")
    }
    w.write("--")
    w.write(boundary)
    w.write("--\r\n")
    w.flush()
  }

  override fun render(e: HttpExchange, code: StatusCode, value: Any?) {
    e.startResponse(code, contentType = fullContentType).use { render(it, value) }
  }
}

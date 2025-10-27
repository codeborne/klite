package klite

import java.io.OutputStream

typealias FormDataRenderer = MultipartRenderer

class MultipartRenderer(
  override val contentType: String = MimeTypes.formData,
  val boundary: String = "----" + System.currentTimeMillis()
): BodyRenderer {
  val fullContentType = "$contentType; boundary=$boundary"

  override fun render(output: OutputStream, value: Any?) = render(output, value as Map<String, Any?>)

  fun render(out: OutputStream, value: Map<String, Any?>) {
    val boundary = "--$boundary".toByteArray()
    value.forEach { (k, v) ->
      if (v == null) return@forEach
      out.write(boundary)
      out.writeln()
      out.write("Content-Disposition: form-data; name=\"$k\"")
      when (v) {
        is FileUpload -> {
          out.write("; filename=\"${v.fileName}\"\r\n")
          out.write("Content-Type: ${MimeTypes.withCharset(v.contentType ?: MimeTypes.unknown)}\r\n\r\n")
          out.flush()
          v.stream.use { it.copyTo(out) }
        }
        else -> {
          out.writeln(); out.writeln()
          if (v is ByteArray) out.write(v) else out.write(v.toString())
        }
      }
      out.writeln()
    }
    out.write(boundary); out.write("--"); out.writeln()
    out.flush()
  }

  override fun render(e: HttpExchange, code: StatusCode, value: Any?) {
    e.startResponse(code, contentType = fullContentType).use { render(it, value) }
  }
}

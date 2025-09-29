package klite

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import kotlin.reflect.KType

@Deprecated("Use FormDataParser")
typealias MultipartFormDataParser = FormDataParser

class FormDataParser: BodyParser {
  override val contentType: String = MimeTypes.formData

  @Suppress("UNCHECKED_CAST")
  override fun <T: Any> parse(input: InputStream, type: KType): T {
    val boundary = input.readLine()!!.trimEnd()
    val result = mutableMapOf<String, Any?>()
    var state = State()
    while (true) {
      val line = input.readLine() ?: break
      if (state.readingHeaders) {
        val lineStr = line.toString(MimeTypes.textCharset).trimEnd()
        val lowerLine = lineStr.lowercase()
        if (lowerLine.isEmpty()) state.readingHeaders = false
        else if (lowerLine.startsWith("content-disposition:")) {
          val disposition = lineStr.substring("content-disposition:".length).trim()
          val params = disposition.split(';').associate(::keyValue)
          state.name = params["name"]
          state.fileName = params["filename"]
        }
        else if (lowerLine.startsWith("content-type:")) {
          state.contentType = lineStr.substring("content-type:".length).trim()
        }
      } else {
        if (line.startsWith(boundary)) {
          if (state.name != null) result[state.name!!] = state.fileName?.let {
            // TODO: remove last newline from content
            FileUpload(it, state.contentType, state.content.toByteArray().trimEnd().inputStream())
          } ?: state.content.toString(MimeTypes.textCharset).trim()
          state = State()
        }
        else state.append(line)
      }
    }
    return result as T
  }

  private fun InputStream.readLine(): ByteArray? {
    val buf = ByteArrayOutputStream(128)
    var b = read()
    while (b >= 0) {
      buf.write(b)
      if (b == '\n'.code) break
      b = read()
    }
    if (b == -1 && buf.size() == 0) return null
    return buf.toByteArray()
  }

  private fun ByteArray.trimEnd(): ByteArray {
    var newLen = size
    if (this[newLen - 1] == 10.toByte()) newLen--
    if (this[newLen - 1] == 13.toByte()) newLen--
    return copyOf(newLen)
  }

  private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    return prefix.size <= size && Arrays.equals(this, 0, prefix.size, prefix, 0, prefix.size)
  }

  private fun keyValue(s: String) = s.split('=', limit = 2).let { it[0].trim() to it.getOrNull(1)?.trim('"') }

  private class State(var readingHeaders: Boolean = true, var name: String? = null, var fileName: String? = null, var contentType: String? = null) {
    val content = ByteArrayOutputStream(4096)
    fun append(line: ByteArray) = content.write(line)
  }
}

class FileUpload(val fileName: String, val contentType: String? = MimeTypes.typeFor(fileName), val stream: InputStream)

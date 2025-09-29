package klite

import java.io.InputStream
import kotlin.reflect.KType
import kotlin.text.Charsets.ISO_8859_1

@Deprecated("Use FormDataParser")
typealias MultipartFormDataParser = FormDataParser

class FormDataParser: BodyParser {
  override val contentType: String = MimeTypes.formData

  @Suppress("UNCHECKED_CAST")
  override fun <T: Any> parse(input: InputStream, type: KType): T {
    val reader = input.bufferedReader(ISO_8859_1)
    val boundary = reader.readLine()
    val result = mutableMapOf<String, Any?>()
    var state = State()
    while (true) {
      val line = reader.readLine() ?: break
      if (state.readingHeaders) {
        val lowerLine = line.lowercase()
        if (lowerLine.isEmpty()) state.readingHeaders = false
        else if (lowerLine.startsWith("content-disposition:")) {
          val disposition = line.substring("content-disposition:".length).trim()
          val params = disposition.split(';').associate(::keyValue)
          state.name = params["name"]
          state.fileName = params["filename"]
        }
        else if (lowerLine.startsWith("content-type:")) {
          state.contentType = line.substring("content-type:".length).trim()
        }
      } else {
        if (line.startsWith(boundary)) {
          if (state.name != null) result[state.name!!] = state.fileName?.let {
            FileUpload(it, state.contentType, state.content.removeSuffix("\n").toString().byteInputStream(ISO_8859_1))
          } ?: state.content.toString().trim()
          state = State()
        }
        else state.append(line)
      }
    }
    return result as T
  }

  private fun keyValue(s: String) = s.split('=', limit = 2).let { it[0].trim() to it.getOrNull(1)?.trim('"') }

  private class State(var readingHeaders: Boolean = true, var name: String? = null, var fileName: String? = null, var contentType: String? = null) {
    val content = StringBuilder()
    fun append(line: String) = content.append(line).append('\n')
  }
}

class FileUpload(val fileName: String, val contentType: String? = MimeTypes.typeFor(fileName), val stream: InputStream)

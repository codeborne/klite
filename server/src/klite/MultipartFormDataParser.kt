package klite

import java.io.InputStream
import kotlin.reflect.KClass

class MultipartFormDataParser(override val contentType: String = "multipart/form-data"): BodyParser {
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> parse(input: InputStream, type: KClass<T>): T {
    val reader = input.bufferedReader()
    val boundary = reader.readLine()
    val result = mutableMapOf<String, Any?>()
    var name: String? = null
    var fileName: String? = null
    var contentType: String? = null
    var readingHeaders = true
    var content = StringBuilder()
    while (true) {
      val line = reader.readLine() ?: break
      if (readingHeaders) {
        val lowerLine = line.lowercase()
        if (lowerLine.isEmpty()) readingHeaders = false
        else if (lowerLine.startsWith("content-disposition:")) {
          val disposition = line.substring("content-disposition:".length).trim()
          val params = disposition.split(';').associate(::keyValue)
          name = params["name"]
          fileName = params["filename"]
        }
        else if (lowerLine.startsWith("content-type:")) {
          contentType = line.substring("content-type:".length).trim()
        }
      } else {
        if (line == boundary) {
          if (name != null) result[name] = fileName?.let {
            FileUpload(it, contentType, content.toString().removeSuffix("\r\n"))
          } ?: content.toString().trim()
          content = StringBuilder()
          readingHeaders = true
          name = null
          fileName = null
          contentType = null
        }
        else content.append(line).append("\r\n")
      }
    }
    return result as T
  }

  private fun keyValue(s: String) = s.split('=', limit = 2).let { it[0].trim() to it.getOrNull(1)?.trim('"') }
}

data class FileUpload(val fileName: String, val contentType: String?, val content: String)

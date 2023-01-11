package klite

import java.nio.file.Path
import kotlin.io.path.extension

object MimeTypes {
  val html = "text/html"
  val text = "text/plain"
  val json = "application/json"
  val xml = "text/xml"
  val pdf = "application/pdf"
  val wwwForm = "application/x-www-form-urlencoded"
  val formData = "multipart/form-data"

  var unknown = "application/octet-stream"
  var textCharset = Charsets.UTF_8
  val byExtension = mutableMapOf(
    "html" to html,
    "txt" to text,
    "xml" to xml,
    "xsd" to xml,
    "js" to "text/javascript",
    "mjs" to "text/javascript",
    "json" to json,
    "css" to "text/css",
    "csv" to "text/csv",
    "ico" to "image/vnd.microsoft.icon",
    "png" to "image/png",
    "gif" to "image/gif",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "webp" to "image/webp",
    "svg" to "image/svg+xml",
    "mp4" to "video/mp4",
    "ogv" to "video/ogv",
    "oga" to "audio/oga",
    "mp3" to "audio/mpeg",
    "ttf" to "font/ttf",
    "otf" to "font/otf",
    "woff" to "font/woff",
    "woff2" to "font/woff2",
    "pdf" to pdf,
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "zip" to "application/zip",
    "jar" to "application/java-archive",
    "gz" to "application/gzip",
    "ics" to "text/calendar",
    "asice" to "application/vnd.etsi.asic-e+zip"
  )

  fun typeFor(file: Path) = byExtension[file.extension]
  fun typeFor(fileName: String) = typeFor(Path.of(fileName))

  fun isText(contentType: String) = contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml")
  fun withCharset(contentType: String) = if (!contentType.contains("charset") && isText(contentType)) "$contentType; charset=$textCharset" else contentType
}

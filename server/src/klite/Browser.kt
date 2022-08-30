package klite

val HttpExchange.browser: String? get() = detectBrowser(header("User-Agent"))

fun detectBrowser(userAgent: String?) = userAgent?.run {
  fun String.detect(browser: String): String? {
    val p = indexOf(browser)
    return if (p >= 0) substring(p, indexOf(' ', startIndex = p + 1).takeIf { it > 0 } ?: length) else null
  }

  (if (contains("Mobile")) "Mobile/" else "") + (
    detect("Edg") ?: detect("Chrome") ?: detect("Firefox") ?: detect("Trident") ?: detect("MSIE") ?:
    if (contains("Safari")) detect("Version")?.replace("Version", "Safari") else split(' ', ';').find { it.contains("bot") } ?: this)
}

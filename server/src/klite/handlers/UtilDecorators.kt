package klite.handlers

import klite.HttpExchange
import klite.NotModifiedException
import klite.RouterConfig
import klite.StatusCode
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

fun RouterConfig.enforceHttps(maxAge: Duration = 365.days) = before {
  if (!isSecure) {
    header("Strict-Transport-Security", "max-age=${maxAge.inWholeSeconds}")
    redirect(fullUrl.toString().replaceFirst("http://", "https://"), StatusCode.PermanentRedirect)
  }
}

fun RouterConfig.enforceCanonicalHost(host: String) = before {
  if (this.host != host) redirect("$protocol://$host$path$query", StatusCode.PermanentRedirect)
}

fun RouterConfig.useHashCodeAsETag() = decorator { e, handler ->
  e.handler()?.also { e.checkETagHashCode(it) }
}

fun HttpExchange.checkETagHashCode(o: Any) {
  if (eTagHashCode(o) == header("If-None-Match")) throw NotModifiedException()
}

fun HttpExchange.eTagHashCode(o: Any) = "W/\"${o.hashCode().toUInt().toString(36)}\"".also { header("ETag", it) }

fun HttpExchange.checkLastModified(at: Instant) {
  if (lastModified(at) == header("If-Modified-Since")) throw NotModifiedException()
}

fun HttpExchange.lastModified(at: Instant): String = DateTimeFormatter.RFC_1123_DATE_TIME.format(at.atOffset(ZoneOffset.UTC)).also {
  header("Last-Modified", it)
}

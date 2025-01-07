package klite.smtp

import klite.MimeTypes
import klite.html.unaryPlus
import klite.i18n.Lang.translate
import org.intellij.lang.annotations.Language
import java.net.URI
import java.util.*

open class EmailContent(val lang: String, val labelKey: String, val substitutions: Map<String, String> = emptyMap(), val actionUrl: URI? = null) {
  open val subject get() = translate(lang, "emails.$labelKey.subject", substitutions)
  open val body get() = translate(lang, "emails.$labelKey.body", substitutions)
  open val actionLabel get() = translate(lang, "emails.$labelKey.action", substitutions)

  override fun equals(other: Any?) = other is EmailContent && lang == other.lang && labelKey == other.labelKey && substitutions == other.substitutions && actionUrl == other.actionUrl
  override fun hashCode() = Objects.hash(lang, labelKey, substitutions, actionUrl)
  override fun toString() = "${this::class.simpleName}($lang, $labelKey, $substitutions, $actionUrl)"

  @Language("html")
  open fun fullHtml() = """
<html lang="$lang">
<head>
  <meta charset="${MimeTypes.textCharset}">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="x-apple-disable-message-reformatting">
  <style>
    body {
      font-family: sans-serif;
      line-height: 1.375em;
      margin: 0;
      padding: 0;
      word-spacing: normal;
      text-align: left;
      text-size-adjust: 100%;
    }

    table {
      border: none;
      border-spacing: 0;
    }
  </style>
</head>
<body>
${contentHtml()}
</body>
</html>
"""

  @Language("html")
  protected open fun contentHtml() = """
<div role="article" aria-roledescription="email" lang="$lang" style="background-color: rgb(243, 244, 246); padding: 1em">
  <table role="presentation" style="width: 94%; max-width: 480px; margin: 0 auto">
    <tr>
      <td style="padding: 2em; background: white; color: rgb(17, 24, 39)">
        <h1 style="margin: 1em 0; font-size: 1.625em; line-height: 1.25; font-weight: bold">${+subject}</h1>
        <div style="margin-bottom: 1em; white-space: pre-line">${+body}</div>
        ${actionUrl?.let {"""
          <a href="$it" style="background: rgb(17, 24, 39); font-weight: bold; text-decoration: none; text-align: center; padding: 1em 2em; color: white; border-radius: 4px; margin-bottom: 1em; display: block">
            $actionLabel
          </a>
        """} ?: ""}
        <p style="margin: 0; font-size: 0.8em; color: rgb(107, 114, 128); text-align: center">
          ${translate(lang, "title")}
        </p>
      </td>
    </tr>
  </table>
</div>
"""
}

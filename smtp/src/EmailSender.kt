package klite.smtp

import klite.*
import klite.i18n.Lang
import klite.i18n.Lang.translate
import javax.mail.internet.InternetAddress

interface EmailSender {
  val defaultFrom: InternetAddress get() = InternetAddress(Config["MAIL_FROM"], Config.optional("MAIL_FROM_NAME", translate(Lang.available.first(), "title")))

  fun send(to: Email, subject: String, body: String, bodyMimeType: String = MimeTypes.text, attachments: Map<String, ByteArray> = emptyMap(), cc: List<Email> = emptyList(), from: InternetAddress = defaultFrom)

  fun send(to: Email, content: EmailContent, attachments: Map<String, ByteArray> = emptyMap(), cc: List<Email> = emptyList()) =
    send(to, content.subject, content.fullHtml(), MimeTypes.html, attachments, cc, content.from ?: defaultFrom)
}

open class FakeEmailSender: EmailSender {
  private val log = logger()
  lateinit var lastSentEmail: String

  override fun send(to: Email, subject: String, body: String, bodyMimeType: String, attachments: Map<String, ByteArray>, cc: List<Email>, from: InternetAddress) {
    lastSentEmail = """
      Email to $to, CC: $cc
      Subject: $subject
      Body ($bodyMimeType): $body
      ${if (attachments.isNotEmpty()) "Attachments: ${attachments.keys}" else ""}
    """
    log.info(lastSentEmail)
  }
}

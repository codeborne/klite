package klite.smtp

import klite.*
import klite.i18n.Lang
import klite.i18n.Lang.translate
import java.util.*
import javax.activation.DataHandler
import javax.mail.Authenticator
import javax.mail.Message.RecipientType.CC
import javax.mail.Message.RecipientType.TO
import javax.mail.Part.ATTACHMENT
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.*
import javax.mail.util.ByteArrayDataSource

interface EmailService {
  val defaultFrom: InternetAddress get() = InternetAddress(Config["MAIL_FROM"], Config.optional("MAIL_FROM_NAME", translate(Lang.available.first(), "title")))

  fun send(to: Email, subject: String, body: String, bodyMimeType: String = MimeTypes.text, attachments: Map<String, ByteArray> = emptyMap(), cc: List<Email> = emptyList(), from: InternetAddress = defaultFrom)

  fun send(to: Email, content: EmailContent, attachments: Map<String, ByteArray> = emptyMap(), cc: List<Email> = emptyList()) =
    send(to, content.subject, content.fullHtml(), MimeTypes.html, attachments, cc, content.from ?: defaultFrom)
}

open class FakeEmailService: EmailService {
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

open class RealEmailService(
  smtpUser: String? = Config.optional("SMTP_USER"),
  smtpPort: String? = Config.optional("SMTP_PORT", "25"),
  props: Properties = Properties().apply {
    this["mail.smtp.host"] = Config.optional("SMTP_HOST", "localhost")
    this["mail.smtp.port"] = smtpPort
    this["mail.smtp.starttls.enable"] = smtpPort == "587"
    this["mail.smtp.auth"] = smtpUser != null
    this["mail.smtp.ssl.protocols"] = "TLSv1.2"
  },
  private val authenticator: Authenticator = object: Authenticator() {
    override fun getPasswordAuthentication() = PasswordAuthentication(smtpUser, Config.required("SMTP_PASS"))
  },
  private val session: Session = Session.getInstance(props, authenticator.takeIf { smtpUser != null })
): EmailService {
  override fun send(to: Email, subject: String, body: String, bodyMimeType: String, attachments: Map<String, ByteArray>, cc: List<Email>, from: InternetAddress) {
    send(to, subject, from) {
      cc.forEach { setRecipient(CC, InternetAddress(it.value)) }
      if (attachments.isEmpty())
        setBody(body, bodyMimeType)
      else
        setContent(MimeMultipart().apply {
          addBodyPart(MimeBodyPart().apply { setBody(body, bodyMimeType) })
          attachments.forEach {
            addBodyPart(MimeBodyPart().apply {
              dataHandler = DataHandler(ByteArrayDataSource(it.value, MimeTypes.typeFor(it.key)))
              fileName = it.key
              disposition = ATTACHMENT
              setHeader("Content-ID", UUID.randomUUID().toString())
            })
          }
        })
    }
  }

  private fun MimePart.setBody(body: String, bodyMimeType: String) = setContent(body, MimeTypes.withCharset(bodyMimeType))

  protected fun send(to: Email, subject: String, from: InternetAddress, block: MimeMessage.() -> Unit) = MimeMessage(session).apply {
    setFrom(from)
    setRecipient(TO, InternetAddress(to.value))
    setSubject(subject, MimeTypes.textCharset.name())
    block()
    Transport.send(this)
  }
}

package klite.smtp

import klite.*
import java.util.*
import javax.activation.DataHandler
import javax.mail.Authenticator
import javax.mail.Message.RecipientType.*
import javax.mail.Part.ATTACHMENT
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.*
import javax.mail.util.ByteArrayDataSource
import kotlin.text.Charsets.UTF_8

interface EmailService {
  fun send(to: Email, subject: String, body: String, bodyMimeType: String = MimeTypes.text, attachments: Map<String, ByteArray> = emptyMap(), cc: List<Email> = emptyList())

  fun send(to: Email, content: EmailContent, attachments: Map<String, ByteArray> = emptyMap(), cc: List<Email> = emptyList()) =
    send(to, content.subject, content.fullHtml(), MimeTypes.html, attachments, cc)
}

class FakeEmailService: EmailService {
  private val log = logger()
  lateinit var lastSentEmail: String

  override fun send(to: Email, subject: String, body: String, bodyMimeType: String, attachments: Map<String, ByteArray>, cc: List<Email>) {
    lastSentEmail = """
      Email to $to, CC: $cc
      Subject: $subject
      Body ($bodyMimeType): $body
      ${if (attachments.isNotEmpty()) "Attachments: ${attachments.keys}" else ""}
    """
    log.info(lastSentEmail)
  }
}

class RealEmailService(
  internal val mailFrom: InternetAddress = InternetAddress(Config["MAIL_FROM"], Config.optional("MAIL_FROM_NAME")),
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
  override fun send(to: Email, subject: String, body: String, bodyMimeType: String, attachments: Map<String, ByteArray>, cc: List<Email>) {
    send(to, subject) {
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

  private fun MimePart.setBody(body: String, bodyMimeType: String) = setContent(body, "$bodyMimeType; charset=UTF-8")

  private fun send(to: Email, subject: String, block: MimeMessage.() -> Unit) = MimeMessage(session).apply {
    setFrom(mailFrom)
    setRecipient(BCC, mailFrom)
    setRecipient(TO, InternetAddress(to.value))
    setSubject(subject, UTF_8.name())
    block()
    Transport.send(this)
  }
}

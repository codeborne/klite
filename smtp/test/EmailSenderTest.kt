package klite.smtp

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.spyk
import io.mockk.verify
import klite.Config
import klite.Email
import klite.MimeTypes
import org.junit.jupiter.api.Test

val email = Email("some@email.com")

class EmailSenderTest {
  init { Config.useEnvFile() }

  @Test fun `instances can be created with default config`() {
    expect(FakeEmailSender()).toBeAnInstanceOf<EmailSender>()
    expect(SmtpEmailSender()).toBeAnInstanceOf<EmailSender>()
  }

  @Test fun translates() {
    val sender = spyk(FakeEmailSender())
    val from = sender.defaultFrom
    expect(from.address).toEqual("klite@codeborne.com")

    val content = EmailContent("en", "hello", mapOf("name" to "World"))
    sender.send(email, content)
    verify { sender.send(email, content.subject, content.fullHtml(), MimeTypes.html, from = from) }
  }
}

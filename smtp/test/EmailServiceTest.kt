package klite.smtp

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.spyk
import io.mockk.verify
import klite.Config
import klite.Email
import klite.MimeTypes
import org.junit.jupiter.api.Test

val email = Email("some@email.com")

class EmailServiceTest {
  init { Config.useEnvFile() }

  @Test fun `instances can be created with default config`() {
    expect(FakeEmailService()).toBeAnInstanceOf<EmailService>()
    expect(RealEmailService()).toBeAnInstanceOf<EmailService>()
  }

  @Test fun translates() {
    val emailService = spyk(FakeEmailService())
    val content = EmailContent("en", "prepayment", mapOf("contractId" to "CONTRACT_ID"))
    emailService.send(email, content)
    verify { emailService.send(email, content.subject, content.fullHtml(), MimeTypes.html) }
  }
}

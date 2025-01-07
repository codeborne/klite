# klite-smtp

Provides a way to send plain text or html email over SMTP using *javax.mail*.

Depends on [klite-i18n](../i18n) for translations.

To use it, initialize the correct implementation when creating the Server instance:
```kotlin
register(if (Config.isDev) FakeEmailSender::class else SmtpEmailSender::class)
```

Add approprate content to your translation files, e.g. `en.json`:
```json
{
  "emails": {
    "welcome": {
      "subject": "Welcome to our service",
      "body": "Hello, {name}! Welcome to our service.",
      "action": "Click here to login"
    }
  }
}
```

Then you can send emails like this:
```kotlin
emailSender.send(Email("recipient@hello.ee"), EmailContent("en", "welcome", mapOf("name" to "John"), URI("https://github.com/login")))
```

You can redefine HTML email template by extending `EmailContent` class.

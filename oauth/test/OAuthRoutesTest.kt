package klite.oauth

import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.*
import klite.i18n.lang
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.*

class OAuthRoutesTest {
  val exchange = mockk<HttpExchange>(relaxed = true) {
    every { fullUrl(any()) } answers { URI("http://host/" + firstArg()) }
    every { redirect(any<URI>()) } throws RedirectException("/")
    every { lang } returns "en"
  }
  val user = UserProfile("GOOGLE", "uid", Email("e@mail"), "Test", "User")
  val token = OAuthTokenResponse("token", 100)
  val oauthClient = mockk<OAuthClient> {
    every { provider } returns user.provider
    coEvery { authenticate("code", any()) } returns token
    coEvery { profile(token) } returns user
  }
  val userCreator = mockk<OAuthUserCreator>()
  val registry = mockk<Registry> {
    every { requireAll<OAuthClient>() } returns listOf(oauthClient)
  }
  val routes = OAuthRoutes(userCreator, registry)

  @Test fun `accept existing user`() {
    every { userCreator.by(user.email) } returns user

    expect { runBlocking { routes.accept("code", "/path", exchange) } }.toThrow<RedirectException>()

    verify {
      exchange.session["userId"] = "uid"
      exchange.redirect(URI("/path"))
    }
  }

  @Test fun `accept new user`() {
    every { userCreator.by(user.email) } returns null
    every { userCreator.create(any(), any(), any()) } returns user

    expect { runBlocking { routes.accept("code", null, exchange) } }.toThrow<RedirectException>()

    verify {
      userCreator.create(user.copy(locale = Locale.ENGLISH), token, exchange)
      exchange.session["userId"] = "uid"
      exchange.redirect(URI("/"))
    }
  }
}

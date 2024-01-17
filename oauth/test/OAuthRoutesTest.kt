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

class OAuthRoutesTest {
  val exchange = mockk<HttpExchange>(relaxed = true) {
    every { fullUrl(any()) } answers { URI("http://host/" + firstArg()) }
    every { redirect(any<URI>()) } throws RedirectException("/")
    every { lang } returns "en"
  }
  val user = UserProfile("GOOGLE", "uid", "Test", "User", Email("e@mail"))
  val token = OAuthTokenResponse("token", 100)
  val oauthClient = mockk<OAuthClient> {
    every { provider } returns user.provider
    coEvery { authenticate("code", any()) } returns token
    coEvery { profile(token) } returns user
  }
  val userRepository = mockk<OAuthUserRepository>()
  val registry = mockk<Registry> {
    every { requireAll<OAuthClient>() } returns listOf(oauthClient)
  }
  val routes = OAuthRoutes(userRepository, registry)

  @Test fun `accept existing user`() {
    every { userRepository.by(user.email) } returns user

    expect { runBlocking { routes.accept("code", "/path", exchange) } }.toThrow<RedirectException>()

    verify {
      exchange.session["userId"] = "uid"
      exchange.redirect(URI("/path"))
    }
  }

  @Test fun `accept new user`() {
    every { userRepository.by(user.email) } returns null
    every { userRepository.create(any(), any(), any()) } returns user

    expect { runBlocking { routes.accept("code", null, exchange) } }.toThrow<RedirectException>()

    verify {
      userRepository.create(user, token, "en")
      exchange.session["userId"] = "uid"
      exchange.redirect(URI("/"))
    }
  }
}

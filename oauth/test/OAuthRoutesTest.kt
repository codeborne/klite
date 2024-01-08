package klite.oauth

import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.Email
import klite.HttpExchange
import klite.RedirectException
import klite.i18n.lang
import klite.oauth.OAuthProvider.GOOGLE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.URI

class OAuthRoutesTest {
  val exchange = mockk<HttpExchange>(relaxed = true) {
    every { fullUrl(any()) } answers { URI("http://host/" + firstArg()) }
    every { redirect(any<URI>()) } throws RedirectException("/")
    every { lang } returns "en"
  }
  val profile = UserProfile(GOOGLE, "gid", "Test", "User", Email("e@mail"))
  val token = OAuthTokenResponse("token", 100)
  val user = mockk<User> {
    every { id } returns "uid"
    every { email } returns profile.email!!
  }
  val oauthClient = mockk<GoogleOAuthClient> {
    coEvery { authenticate("code", any()) } returns token
    coEvery { profile("token") } returns profile
  }
  val userRepository = mockk<UserRepository>()
  val routes = OAuthRoutes(oauthClient, userRepository)

  @Test fun `accept existing user`() {
    every { userRepository.by(profile.email!!) } returns user

    expect { runBlocking { routes.accept("code", "/path", exchange) } }.toThrow<RedirectException>()

    verify {
      exchange.session["userId"] = "uid"
      exchange.redirect(URI("/path"))
    }
  }

  @Test fun `accept new user`() {
    every { userRepository.by(profile.email!!) } returns null
    every { userRepository.create(any(), any(), any()) } returns user

    expect { runBlocking { routes.accept("code", null, exchange) } }.toThrow<RedirectException>()

    verify {
      userRepository.create(profile, token, "en")
      exchange.session["userId"] = "uid"
      exchange.redirect(URI("/"))
    }
  }
}

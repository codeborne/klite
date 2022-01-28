package klite

import klite.Cookie.SameSite.None
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant.ofEpochSecond

class CookieTest {
  @Test fun plain() {
    expectThat(Cookie("Hello", "Wörld").toString()).isEqualTo("Hello=W%C3%B6rld")
  }

  @Test fun expires() {
    expectThat(Cookie("Hello", "World", expires = ofEpochSecond(1634800860)).toString()).isEqualTo("Hello=World; Expires=Thu, 21 Oct 2021 07:21:00 GMT")
  }

  @Test fun domain() {
    expectThat(Cookie("Hello", "World", domain = "github.com").toString()).isEqualTo("Hello=World; Domain=github.com")
  }

  @Test fun path() {
    expectThat(Cookie("Hello", "World", path = "/hello").toString()).isEqualTo("Hello=World; Path=/hello")
  }

  @Test fun attrs() {
    expectThat(Cookie("Hello", "World", httpOnly = true, secure = true, sameSite = None).toString()).isEqualTo("Hello=World; HttpOnly; Secure; SameSite=None")
  }
}

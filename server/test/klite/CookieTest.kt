package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.Cookie.SameSite.None
import org.junit.jupiter.api.Test
import java.time.Instant.ofEpochSecond

class CookieTest {
  @Test fun plain() {
    expect(Cookie("Hello", "Wörld").toString()).toEqual("Hello=W%C3%B6rld")
  }

  @Test fun expires() {
    expect(Cookie("Hello", "World", expires = ofEpochSecond(1634800860)).toString()).toEqual("Hello=World; Expires=Thu, 21 Oct 2021 07:21:00 GMT")
  }

  @Test fun domain() {
    expect(Cookie("Hello", "World", domain = "github.com").toString()).toEqual("Hello=World; Domain=github.com")
  }

  @Test fun path() {
    expect(Cookie("Hello", "World", path = "/hello").toString()).toEqual("Hello=World; Path=/hello")
  }

  @Test fun attrs() {
    expect(Cookie("Hello", "World", httpOnly = true, secure = true, sameSite = None).toString()).toEqual("Hello=World; HttpOnly; Secure; SameSite=None")
  }
}

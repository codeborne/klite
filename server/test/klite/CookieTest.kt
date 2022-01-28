package klite

import klite.Cookie.SameSite.None
import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test
import java.time.Instant.ofEpochSecond

class CookieTest {
  @Test fun plain() {
    expect(Cookie("Hello", "Wörld").toString()).to.equal("Hello=W%C3%B6rld")
  }

  @Test fun expires() {
    expect(Cookie("Hello", "World", expires = ofEpochSecond(1634800860)).toString()).to.equal("Hello=World; Expires=Thu, 21 Oct 2021 07:21:00 GMT")
  }

  @Test fun domain() {
    expect(Cookie("Hello", "World", domain = "github.com").toString()).to.equal("Hello=World; Domain=github.com")
  }

  @Test fun path() {
    expect(Cookie("Hello", "World", path = "/hello").toString()).to.equal("Hello=World; Path=/hello")
  }

  @Test fun attrs() {
    expect(Cookie("Hello", "World", httpOnly = true, secure = true, sameSite = None).toString()).to.equal("Hello=World; HttpOnly; Secure; SameSite=None")
  }
}

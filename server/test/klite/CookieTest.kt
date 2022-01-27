package klite

import klite.Cookie.SameSite.None
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant.ofEpochSecond

class CookieTest {
  @Test fun plain() {
    assertThat(Cookie("Hello", "Wörld").toString()).isEqualTo("Hello=W%C3%B6rld")
  }

  @Test fun expires() {
    assertThat(Cookie("Hello", "World", expires = ofEpochSecond(1634800860)).toString()).isEqualTo("Hello=World; Expires=Thu, 21 Oct 2021 07:21:00 GMT")
  }

  @Test fun domain() {
    assertThat(Cookie("Hello", "World", domain = "github.com").toString()).isEqualTo("Hello=World; Domain=github.com")
  }

  @Test fun path() {
    assertThat(Cookie("Hello", "World", path = "/hello").toString()).isEqualTo("Hello=World; Path=/hello")
  }

  @Test fun attrs() {
    assertThat(Cookie("Hello", "World", httpOnly = true, secure = true, sameSite = None).toString()).isEqualTo("Hello=World; HttpOnly; Secure; SameSite=None")
  }
}

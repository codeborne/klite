import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.Converter
import klite.base64UrlDecode
import klite.oauth.JWT
import org.junit.jupiter.api.Test
import java.time.Instant

class JWTTest {
  val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

  @Test fun parse() {
    val jwt = JWT(token)
    expect(jwt.header.alg).toEqual("HS256")
    expect(jwt.header.typ).toEqual("JWT")
    expect(jwt.payload.subject).toEqual("1234567890")
    expect(jwt.payload.name).toEqual("John Doe")
    expect(jwt.payload.issuedAt).toEqual(Instant.ofEpochSecond(1516239022))
    expect(jwt.signature).toEqual("SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c".base64UrlDecode().decodeToString())
  }

  @Test fun converter() {
    val jwt = JWT(token)
    expect(Converter.from<JWT>(token)).toEqual(jwt)
  }
}

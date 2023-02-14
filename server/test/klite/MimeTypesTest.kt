package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class MimeTypesTest {
  @Test fun withCharset() {
    expect(MimeTypes.withCharset(MimeTypes.typeFor("x.txt")!!)).toEqual("text/plain; charset=UTF-8")
    expect(MimeTypes.withCharset("text/something; charset=ISO-8859-1")).toEqual("text/something; charset=ISO-8859-1")
  }
}

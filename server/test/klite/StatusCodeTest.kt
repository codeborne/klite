package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class StatusCodeTest {
  @Test fun reasons() {
    expect(StatusCode.OK.reason).toEqual("OK")
    expect(StatusCode.BadRequest.reason).toEqual("Bad Request")
  }
}

package klite


import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.http.TypedHttpClient
import org.junit.jupiter.api.Test
import java.net.http.HttpClient

class TypedHttpClientLoggerNameTest {
  @Test fun loggerNameShouldReferToClassName() {
    expect(
      TypedHttpClient(
        urlPrefix = "",
        http = HttpClient.newHttpClient(),
        contentType = ""
      ).logger.name
    ).toEqual("klite.http.TypedHttpClient")
  }

  @Test fun loggerNameShouldReferToDerivedClassName() {
    expect(DerivedTypedHttpClient().logger.name).toEqual("klite.DerivedTypedHttpClient")
  }
}

private class DerivedTypedHttpClient: TypedHttpClient(
  urlPrefix = "",
  http = HttpClient.newHttpClient(),
  contentType = "",
)

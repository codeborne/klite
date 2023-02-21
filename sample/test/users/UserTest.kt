package users

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.json.JsonMapper
import klite.json.parse
import org.junit.jupiter.api.Test

class UserTest {
  val json = JsonMapper()

  @Test fun json() {
    val address = User.Address(Id("d7242ad0-11ea-4991-93af-7f9e08784bce"), "Tallinn", "EE", Id("a7f033bc-04f3-4cc0-94f1-564783cab08f"))
    val addressJson = json.render(address)
    expect(addressJson).toEqual("""{"userId":"d7242ad0-11ea-4991-93af-7f9e08784bce","city":"Tallinn","countryCode":"EE","id":"a7f033bc-04f3-4cc0-94f1-564783cab08f"}""")
    expect(json.parse<User.Address>(addressJson)).toEqual(address)
  }
}

package users

import klite.Email
import users.User.Address
import java.util.Locale.ENGLISH

object TestData {
  val address = Address(Id("d7242ad0-11ea-4991-93af-7f9e08784bce"), "Tallinn", "EE", Id("a7f033bc-04f3-4cc0-94f1-564783cab08f"))
  val user = User(Email("john@doe.com"), "John", "Doe", ENGLISH, "hash", null, Id("4024d2bc-ebb1-11ef-b470-0fea3b49cda0"))
}

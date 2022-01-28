package klite.jdbc.users

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import users.Email
import users.User
import users.UserRepository
import java.util.*

class UserRepositoryTest: DBTest() {
  val user = User(Email("test@user.com"), "Test", "User", Locale.ENGLISH, "phash")
  val repository = UserRepository(db)

  @Test fun `save and load`() {
    repository.save(user)
    expect(repository.get(user.id)).toEqual(user)
    expect(repository.by(user.email)).toEqual(user)
    expect(repository.by(Email("no@email"))).toEqual(null)
  }
}

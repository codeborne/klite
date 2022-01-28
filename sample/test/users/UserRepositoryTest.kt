package klite.jdbc.users

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import users.Email
import users.User
import users.UserRepository
import java.util.*

class UserRepositoryTest: DBTest() {
  val user = User(Email("test@user.com"), "Test", "User", Locale.ENGLISH, "phash")
  val repository = UserRepository(db)

  @Test fun `save and load`() {
    repository.save(user)
    expectThat(repository.get(user.id)).isEqualTo(user)
    expectThat(repository.by(user.email)).isEqualTo(user)
    expectThat(repository.by(Email("no@email"))).isNull()
  }
}

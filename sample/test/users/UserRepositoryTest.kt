package klite.jdbc.users

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import users.Email
import users.User
import users.UserRepository
import java.util.*

class UserRepositoryTest: DBTest() {
  val user = User(Email("test@user.com"), "Test", "User", Locale.ENGLISH, "phash")
  val repository = UserRepository(db)

  @Test
  fun `save and load`() {
    repository.save(user)
    assertThat(repository.get(user.id)).isEqualTo(user)
    assertThat(repository.by(user.email)).isEqualTo(user)
    assertThat(repository.by(Email("no@email"))).isNull()
  }
}

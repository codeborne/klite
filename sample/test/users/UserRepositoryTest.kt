package klite.jdbc.users

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beNull
import net.oddpoet.expect.extension.equal
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
    expect(repository.get(user.id)).to.equal(user)
    expect(repository.by(user.email)).to.equal(user)
    expect(repository.by(Email("no@email"))).to.beNull()
  }
}

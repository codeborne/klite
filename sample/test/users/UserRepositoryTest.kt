package klite.sample.users

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import klite.jdbc.StaleEntityException
import klite.sample.DBTest
import org.junit.jupiter.api.Test
import users.Email
import users.User
import users.UserRepository
import java.util.Locale.ENGLISH

class UserRepositoryTest: DBTest() {
  val user = User(Email("test@user.com"), "Test", "User", ENGLISH, "phash")
  val repository = UserRepository(db)

  @Test fun `save and load`() {
    repository.save(user)
    expect(user.id).notToEqualNull()
    repository.save(user)

    expect(repository.get(user.id!!)).toEqual(user)
    expect(repository.by(user.email)).toEqual(user)
    expect(repository.by(Email("no@email"))).toEqual(null)

    expect(repository.search("SER")).toContain(user)
    expect(repository.search("blah")).toBeEmpty()
  }

  @Test fun updatedAt() {
    val oldUpdatedAt = user.updatedAt
    repository.save(user)
    expect(user.updatedAt).notToEqual(oldUpdatedAt)
    repository.save(user)

    user.updatedAt = user.updatedAt!!.plusMillis(123)
    expect { repository.save(user) }.toThrow<StaleEntityException>()
  }
}

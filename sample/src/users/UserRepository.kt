package users

import klite.jdbc.CrudRepository
import javax.sql.DataSource

class UserRepository(db: DataSource): CrudRepository<User>(db, "users") {
  fun by(email: Email): User? = list(User::email to email).firstOrNull()
}

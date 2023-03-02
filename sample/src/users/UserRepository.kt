package users

import klite.jdbc.ilike
import klite.jdbc.or
import javax.sql.DataSource

class UserRepository(db: DataSource): CrudRepository<User>(db, "users") {
  fun by(email: Email): User? = list(User::email to email).firstOrNull()
  fun search(q: String) = list(or(User::firstName ilike "%$q%", User::lastName ilike "%$q%", User::email ilike "%$q%"))
}

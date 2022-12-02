package users

import klite.jdbc.CrudRepository
import klite.jdbc.query
import javax.sql.DataSource

class UserRepository(db: DataSource): CrudRepository<User>(db, "users") {
  fun by(email: Email): User? = db.query(table, mapOf("email" to email)) { mapper() }.firstOrNull()
}

package users

import klite.jdbc.*
import java.util.*
import javax.sql.DataSource

class UserRepository(db: DataSource) : BaseRepository(db, "users") {
  fun save(user: User) = db.insert(table, user.toValues())
  fun get(id: UUID): User = db.query(table, id) { fromValues() }
  fun by(email: Email): User? = db.query(table, mapOf("email" to email)) { fromValues<User>() }.firstOrNull()
}

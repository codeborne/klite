package users

import klite.jdbc.Persistable
import java.util.*

@JvmInline value class Email(val email: String)

data class User(
  val email: Email,
  val firstName: String,
  val lastName: String,
  val locale: Locale,
  val passwordHash: String
): Persistable<User>()

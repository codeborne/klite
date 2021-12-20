package users

import klite.jdbc.BaseModel
import java.util.*
import java.util.UUID.randomUUID

@JvmInline value class Email(val email: String)

data class User(
  val email: Email,
  val firstName: String,
  val lastName: String,
  val locale: Locale,
  val passwordHash: String,
  override val id: UUID = randomUUID()
): BaseModel

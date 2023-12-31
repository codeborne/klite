package users

import klite.jdbc.UpdatableEntity
import java.time.Instant
import java.util.*

@JvmInline value class Email(val email: String)

data class User(
  val email: Email,
  val firstName: String,
  val lastName: String,
  val locale: Locale,
  val passwordHash: String? = null,
  override val id: Id<User> = Id(),
  override var updatedAt: Instant? = null
): Entity<User>, UpdatableEntity {
  data class Address(
    val userId: Id<User>,
    val city: String,
    val countryCode: String,
    override val id: Id<Address> = Id()
  ): Entity<Address>
}

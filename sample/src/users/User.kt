package users

import java.util.*

@JvmInline value class Email(val email: String)

data class User(
  val email: Email,
  val firstName: String,
  val lastName: String,
  val locale: Locale,
  val passwordHash: String,
  override val id: Id<User> = Id()
): Entity<User> {
  data class Address(
    val userId: Id<User>,
    val city: String,
    val countryCode: String,
    override val id: Id<Address> = Id()
  ): Entity<Address>
}

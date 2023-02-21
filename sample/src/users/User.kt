package users

import klite.jdbc.BaseEntity
import klite.jdbc.toId
import java.util.*

@JvmInline value class Email(val email: String)
@JvmInline value class Id<in T>(val uuid: UUID) {
  constructor(uuid: String): this(uuid.toId())
}

data class User(
  val email: Email,
  val firstName: String,
  val lastName: String,
  val locale: Locale,
  val passwordHash: String,
  override val id: Id<User> = Id(UUID.randomUUID())
): BaseEntity<Id<User>> {
  data class Address(
    val userId: Id<User>,
    val city: String,
    val countryCode: String,
    override val id: Id<Address> = Id(UUID.randomUUID())
  ): BaseEntity<Id<Address>>
}

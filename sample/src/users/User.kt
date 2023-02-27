package users

import klite.Converter
import klite.jdbc.BaseEntity
import klite.uuid
import java.util.*

@JvmInline value class Email(val email: String)

@JvmInline value class Id<T>(val uuid: UUID = UUID.randomUUID()) {
  constructor(uuid: String): this(uuid.uuid)
  override fun toString() = uuid.toString()
  companion object {
    init { Converter.use { Id<Any>(it) } }
  }
}

data class User(
  val email: Email,
  val firstName: String,
  val lastName: String,
  val locale: Locale,
  val passwordHash: String,
  override val id: Id<User> = Id()
): BaseEntity<Id<User>> {
  data class Address(
    val userId: Id<User>,
    val city: String,
    val countryCode: String,
    override val id: Id<Address> = Id()
  ): BaseEntity<Id<Address>>
}

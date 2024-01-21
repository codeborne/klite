package users

import klite.Email
import klite.jdbc.UpdatableEntity
import klite.oauth.OAuthUser
import java.net.URI
import java.time.Instant
import java.util.*

data class User(
  override val email: Email,
  override val firstName: String,
  override val lastName: String,
  val locale: Locale,
  val passwordHash: String? = null,
  val avatarUrl: URI? = null,
  override var id: Id<User>? = null,
  override var updatedAt: Instant? = null
): Entity<User>, UpdatableEntity, OAuthUser {
  data class Address(
    val userId: Id<User>,
    val city: String,
    val countryCode: String,
    override var id: Id<Address>? = null,
    override var updatedAt: Instant? = null
  ): Entity<Address>
}

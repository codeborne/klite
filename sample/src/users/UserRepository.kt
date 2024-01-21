package users

import klite.Email
import klite.HttpExchange
import klite.jdbc.ilike
import klite.jdbc.or
import klite.oauth.OAuthTokenResponse
import klite.oauth.OAuthUserCreator
import klite.oauth.UserProfile
import java.util.Locale.ENGLISH
import javax.sql.DataSource

class UserRepository(db: DataSource): CrudRepository<User>(db, "users"), OAuthUserCreator {
  fun search(q: String) = list(or(User::firstName ilike "%$q%", User::lastName ilike "%$q%", User::email ilike "%$q%"))

  override fun by(email: Email): User? = list(User::email to email).firstOrNull()

  override fun create(profile: UserProfile, tokenResponse: OAuthTokenResponse, exchange: HttpExchange) =
    User(profile.email, profile.firstName, profile.lastName, profile.locale ?: ENGLISH, avatarUrl = profile.avatarUrl).also {
      save(it)
    }
}

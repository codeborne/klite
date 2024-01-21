package klite.oauth

import klite.Email
import klite.HttpExchange

interface OAuthUser {
  val email: Email
  val firstName: String
  val lastName: String
  val id: Any?
}

interface OAuthUserCreator {
  fun by(email: Email): OAuthUser?
  fun create(profile: UserProfile, tokenResponse: OAuthTokenResponse, exchange: HttpExchange): OAuthUser
}

package klite.oauth

import klite.Email

interface OAuthUser {
  val email: Email
  val firstName: String
  val lastName: String
  val id: Any?
}

interface OAuthUserRepository {
  fun by(email: Email): OAuthUser?
  fun create(profile: UserProfile, tokenResponse: OAuthTokenResponse): OAuthUser
}

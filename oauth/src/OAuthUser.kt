package klite.oauth

import klite.Email
import java.net.URI

interface OAuthUser {
  val email: Email
  val firstName: String
  val lastName: String
  val avatarUrl: URI?
  val id: Any?
}

interface OAuthUserRepository {
  fun by(email: Email): OAuthUser?
  fun create(profile: UserProfile, tokenResponse: OAuthTokenResponse, lang: String): OAuthUser
}

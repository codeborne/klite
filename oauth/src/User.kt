package klite.oauth

import klite.Email
import java.net.URI

interface User {
  val email: Email
  val firstName: String
  val lastName: String
  val avatarUrl: URI?
  val id: Any?
}

interface UserRepository {
  fun by(email: Email): User?
  fun create(profile: UserProfile, tokenResponse: OAuthTokenResponse, lang: String): User
}

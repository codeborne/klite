package klite.oauth

import klite.*
import klite.json.JsonHttpClient
import klite.json.JsonMapper
import klite.json.SnakeCase
import java.net.URI
import java.net.http.HttpClient

class OAuthClient(
  val provider: OAuthProvider,
  httpClient: HttpClient
) {
  private val http = JsonHttpClient(json = JsonMapper(keys = SnakeCase), http = httpClient)
  private val clientId = Config["${provider}_OAUTH_CLIENT_ID"]
  private val clientSecret = Config["${provider}_OAUTH_CLIENT_SECRET"]
  private val scope = Config.optional("${provider}_OAUTH_SCOPE", "email profile")

  fun startAuthUrl(state: String?, redirectUrl: URI, lang: String) = URI(provider.authUrl) + mapOfNotNull(
    "response_type" to "code",
    "response_mode" to "form_post",
    "redirect_uri" to redirectUrl,
    "client_id" to clientId,
    "scope" to scope,
    "access_type" to "offline",
    "state" to state,
    "hl" to lang,
    "prompt" to "select_account"
  )

  suspend fun authenticate(code: String, redirectUrl: URI) = fetchTokenResponse("authorization_code", code, redirectUrl)
  suspend fun refresh(refreshToken: String) = fetchTokenResponse("refresh_token", refreshToken)

  private suspend fun fetchTokenResponse(grantType: String, code: String, redirectUrl: URI? = null): OAuthTokenResponse =
    http.post(provider.tokenUrl, urlEncodeParams(mapOf(
      "grant_type" to grantType,
      (if (grantType == "authorization_code") "code" else grantType) to code,
      "client_id" to clientId,
      "client_secret" to clientSecret,
      "redirect_uri" to redirectUrl?.toString()
    ))) {
      setHeader("Content-Type", "${MimeTypes.wwwForm}; charset=UTF-8")
    }

  suspend fun profile(token: OAuthTokenResponse) = provider.fetchProfile(http, token)
}

enum class OAuthProvider(val authUrl: String, val tokenUrl: String, val profileUrl: String, val fetchProfile: suspend (http: JsonHttpClient, token: OAuthTokenResponse) -> UserProfile) {
  GOOGLE(
    Config.optional("GOOGLE_OAUTH_URL", "https://accounts.google.com/o/oauth2/v2/auth"),
    Config.optional("GOOGLE_OAUTH_TOKEN_URL", "https://oauth2.googleapis.com/token"),
    Config.optional("GOOGLE_OAUTH_PROFILE_URL", "https://www.googleapis.com/oauth2/v1/userinfo?alt=json"),
    { http, token ->
      val res = http.get<Map<String, String>>(GOOGLE.profileUrl) { setHeader("Authorization", "Bearer " + token.accessToken) }
      val firstName = res["givenName"] ?: ""
      val lastName = res["familyName"] ?: ""
      UserProfile(GOOGLE, res["id"]!!, firstName, lastName, res["email"]?.let { Email(it) }, res["picture"]?.let { URI(it) })
    })
}

data class OAuthTokenResponse(val accessToken: String, val expiresIn: Int, val scope: String? = null, val tokenType: String? = null, val idToken: String? = null, val refreshToken: String? = null)
data class UserProfile(val provider: OAuthProvider, val id: String, val firstName: String, val lastName: String, val email: Email?, val avatarUrl: URI? = null)

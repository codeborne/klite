package klite.oauth

import klite.*
import klite.json.JsonHttpClient
import klite.json.JsonMapper
import klite.json.SnakeCase
import klite.oauth.OAuthProvider.GOOGLE
import java.net.URI
import java.net.http.HttpClient

abstract class OAuthClient(
  val provider: OAuthProvider,
  httpClient: HttpClient
) {
  private val http: JsonHttpClient = JsonHttpClient(json = JsonMapper(keys = SnakeCase), http = httpClient)
  private val clientId = Config["${provider}_OAUTH_CLIENT_ID"]
  private val clientSecret = Config["${provider}_OAUTH_CLIENT_SECRET"]

  private val scope = Config["${provider}_OAUTH_SCOPE"]
  private val authUrl = URI(Config["${provider}_OAUTH_URL"])
  private val tokenUrl = Config["${provider}_OAUTH_TOKEN_URL"]
  private val profileUrl = Config["${provider}_OAUTH_PROFILE_URL"]

  fun startAuthUrl(state: String?, redirectUrl: URI, lang: String) = authUrl + mapOfNotNull(
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
    http.post(tokenUrl, urlEncodeParams(mapOf(
      "grant_type" to grantType,
      (if (grantType == "authorization_code") "code" else grantType) to code,
      "client_id" to clientId,
      "client_secret" to clientSecret,
      "redirect_uri" to redirectUrl?.toString()
    ))) {
      setHeader("Content-Type", "${MimeTypes.wwwForm}; charset=UTF-8")
    }

  protected suspend fun profileRequest(accessToken: String): Map<String, String> =
    http.get(profileUrl) {
      setHeader("Authorization", "Bearer $accessToken")
    }

  abstract suspend fun profile(accessToken: String): UserProfile
}

enum class OAuthProvider {
  GOOGLE
}

class GoogleOAuthClient(httpClient: HttpClient): OAuthClient(GOOGLE, httpClient) {
  override suspend fun profile(accessToken: String): UserProfile {
    val res = profileRequest(accessToken)
    val firstName = res["givenName"] ?: ""
    val lastName = res["familyName"] ?: ""
    return UserProfile(provider, res["id"]!!, firstName, lastName, res["email"]?.let { Email(it) }, res["picture"]?.let { URI(it) })
  }
}

data class OAuthTokenResponse(val accessToken: String, val expiresIn: Int, val scope: String? = null, val tokenType: String? = null, val idToken: String? = null, val refreshToken: String? = null)
data class UserProfile(val provider: OAuthProvider, val id: String, val firstName: String, val lastName: String, val email: Email?, val avatarUrl: URI? = null)

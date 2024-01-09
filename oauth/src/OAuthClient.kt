package klite.oauth

import klite.*
import klite.http.authBearer
import klite.json.*
import java.net.URI
import java.net.http.HttpClient

class OAuthClient(
  val provider: OAuthProvider,
  httpClient: HttpClient
) { // TODO: will be nice to support custom subclasses
  private val http = JsonHttpClient(json = JsonMapper(keys = SnakeCase, trimToNull = false), http = httpClient)
  private val clientId = Config["${provider}_OAUTH_CLIENT_ID"]
  private val clientSecret = Config["${provider}_OAUTH_CLIENT_SECRET"]

  fun startAuthUrl(state: String?, redirectUrl: URI, lang: String) = URI(provider.authUrl) + mapOfNotNull(
    "response_type" to "code",
    "response_mode" to "form_post",
    "redirect_uri" to redirectUrl,
    "client_id" to clientId,
    "scope" to provider.scope,
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
      setHeader("Content-Type", MimeTypes.withCharset(MimeTypes.wwwForm))
    }

  suspend fun profile(token: OAuthTokenResponse) = provider.fetchProfile(http, token)
}

enum class OAuthProvider(val scope: String, val authUrl: String, val tokenUrl: String, val profileUrl: String, val fetchProfile: suspend (http: JsonHttpClient, token: OAuthTokenResponse) -> UserProfile) {
  // https://console.cloud.google.com/apis/credentials
  GOOGLE(
    Config.optional("GOOGLE_OAUTH_SCOPE", "email profile"),
    Config.optional("GOOGLE_OAUTH_URL", "https://accounts.google.com/o/oauth2/v2/auth"),
    Config.optional("GOOGLE_OAUTH_TOKEN_URL", "https://oauth2.googleapis.com/token"),
    Config.optional("GOOGLE_OAUTH_PROFILE_URL", "https://www.googleapis.com/oauth2/v1/userinfo?alt=json"),
    { http, token ->
      val res = http.get<Map<String, String>>(GOOGLE.profileUrl) { authBearer(token.accessToken) }
      UserProfile(GOOGLE, res["id"]!!, res["givenName"] ?: "", res["familyName"] ?: "", Email(res["email"]!!), res["picture"]?.let { URI(it) })
    }
  ),
  APPLE(
    Config.optional("APPLE_OAUTH_SCOPE", "email name"),
    Config.optional("APPLE_OAUTH_URL", "https://appleid.apple.com/auth/authorize"),
    Config.optional("APPLE_OAUTH_TOKEN_URL", "https://appleid.apple.com/auth/token"),
    "",
    { _, token ->
      val email = token.idToken?.let {
        val payload = it.split(".")[1]
        val decodedPayload = payload.base64Decode()
        JsonMapper().parse<JsonNode>(String(decodedPayload)).getString("email")
      }
      TODO()
    }
  ),
  // https://portal.azure.com/
  MICROSOFT(
    Config.optional("MICROSOFT_OAUTH_SCOPE", "email openid offline_access User.Read"),
    Config.optional("MICROSOFT_OAUTH_URL", "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"),
    Config.optional("MICROSOFT_OAUTH_TOKEN_URL", "https://login.microsoftonline.com/common/oauth2/v2.0/token"),
    Config.optional("MICROSOFT_OAUTH_PROFILE_URL", "https://graph.microsoft.com/v1.0/me"),
    { http, token ->
      val res = http.get<JsonNode>(MICROSOFT.profileUrl) { authBearer(token.accessToken) }
      val email = res.getOrNull<String>("mail") ?: res.getOrNull<String>("userPrincipalName") ?: error("Cannot obtain user's email")
      UserProfile(MICROSOFT, res.getString("id"), res.getString("givenName"), res.getString("surname"), Email(email))
    }
  ),
  // https://developers.facebook.com/apps/
  FACEBOOK(
    Config.optional("FACEBOOK_OAUTH_SCOPE", "email public_profile"),
    Config.optional("FACEBOOK_OAUTH_URL", "https://www.facebook.com/v12.0/dialog/oauth"),
    Config.optional("FACEBOOK_OAUTH_TOKEN_URL", "https://graph.facebook.com/v12.0/oauth/access_token"),
    Config.optional("FACEBOOK_OAUTH_PROFILE_URL", "https://graph.facebook.com/v12.0/me?fields=id,first_name,last_name,email,picture"),
    { http, token ->
      val res = http.get<JsonNode>(FACEBOOK.profileUrl) { authBearer(token.accessToken) }
      val avatarData = res.getOrNull<JsonNode>("picture")?.getOrNull<JsonNode>("data")
      val avatarExists = avatarData?.getOrNull<Boolean>("is_silhouette") != true
      UserProfile(FACEBOOK, res.getString("id"), res.getString("firstName"), res.getString("lastName"), Email(res.getString("email")), avatarData?.getOrNull<String>("url")?.takeIf { avatarExists }?.let { URI(it) })
    }
  )
}

data class OAuthTokenResponse(val accessToken: String, val expiresIn: Int, val scope: String? = null, val tokenType: String? = null, val idToken: String? = null, val refreshToken: String? = null)
data class UserProfile(val provider: OAuthProvider, override val id: String, override val firstName: String, override val lastName: String, override val email: Email, override val avatarUrl: URI? = null): OAuthUser

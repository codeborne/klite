package klite.oauth

import klite.Config
import klite.HttpExchange
import klite.annotations.BodyParam
import klite.annotations.GET
import klite.annotations.POST
import klite.annotations.Path
import klite.i18n.lang
import klite.mapOfNotNull
import klite.plus
import java.net.URI

const val oauthPath = "/login/oauth"

@Path(oauthPath)
open class OAuthRoutes(
  private val oauthClient: GoogleOAuthClient,
  private val userRepository: UserRepository
) {
  @GET open suspend fun start(e: HttpExchange) =
    if (e.query("error_message") != null) e.redirectToLogin(null, "oauthProviderRefused")
    else if (e.query("state") != null) accept(e.query("code"), e.query("state")!!, e)
    else e.redirect(oauthClient.startAuthUrl(e.safeRedirectParam?.toString(), e.fullUrl("/api$oauthPath"), e.lang))

  @POST open suspend fun accept(@BodyParam code: String?, @BodyParam state: String?, e: HttpExchange) {
    val originalUrl = state?.let { URI(it) }
    if (code == null) e.redirectToLogin(originalUrl, "userCancelled")

    val tokenResponse = oauthClient.authenticate(code, e.fullUrl("/api$oauthPath"))
    val profile = oauthClient.profile(tokenResponse.accessToken)
    if (profile.email == null) e.redirectToLogin(originalUrl, "emailNotProvided")

    val user = userRepository.by(profile.email) ?: userRepository.create(profile, tokenResponse, e.lang)
    e.initSession(user)
    e.redirect(originalUrl ?: URI("/"))
  }
}

val origin = Config.optional("ORIGIN") ?: ""
val HttpExchange.safeRedirectParam get() = query("redirect")?.takeIf { it.startsWith("/") || it.startsWith(origin) }?.let { URI(it) }

fun HttpExchange.redirectToLogin(originalUrl: URI? = fullUrl, errorKey: String? = null): Nothing =
  redirect(fullUrl("/api$oauthPath") + mapOfNotNull("redirect" to originalUrl, "errorKey" to errorKey))

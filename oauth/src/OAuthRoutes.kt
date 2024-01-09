package klite.oauth

import klite.Config
import klite.HttpExchange
import klite.annotations.BodyParam
import klite.annotations.GET
import klite.annotations.POST
import klite.annotations.PathParam
import klite.i18n.lang
import klite.mapOfNotNull
import klite.plus
import java.net.URI
import java.net.http.HttpClient
import java.util.concurrent.ConcurrentHashMap

open class OAuthRoutes(
  private val userRepository: OAuthUserRepository,
  private val httpClient: HttpClient
) {
  private val clients = ConcurrentHashMap<OAuthProvider, OAuthClient>()
  private fun client(provider: OAuthProvider) = clients.getOrPut(provider) { OAuthClient(provider, httpClient) }

  @GET("/:provider") open suspend fun start(@PathParam provider: OAuthProvider, e: HttpExchange) =
    if (e.query("error_message") != null) e.redirectToLogin(null, "oauthProviderRefused")
    else if (e.query("state") != null) accept(provider, e.query("code"), e.query("state")!!, e)
    else e.redirect(client(provider).startAuthUrl(e.safeRedirectParam?.toString(), e.fullUrl(e.contextPath), e.lang))

  @POST("/:provider") open suspend fun accept(@PathParam provider: OAuthProvider, @BodyParam code: String?, @BodyParam state: String?, e: HttpExchange) {
    val originalUrl = state?.let { URI(it) }
    if (code == null) e.redirectToLogin(originalUrl, "userCancelled")

    val client = client(provider)
    val token = client.authenticate(code, e.fullUrl(e.contextPath))
    val profile = client.profile(token)

    val user = userRepository.by(profile.email) ?: userRepository.create(profile, token, e.lang)
    e.initSession(user)
    e.redirect(originalUrl ?: URI("/"))
  }
}

val origin = Config.optional("ORIGIN") ?: ""
val HttpExchange.safeRedirectParam get() = query("redirect")?.takeIf { it.startsWith("/") || it.startsWith(origin) }?.let { URI(it) }

private fun HttpExchange.redirectToLogin(originalUrl: URI? = fullUrl, errorKey: String? = null): Nothing =
  redirect(fullUrl(contextPath) + mapOfNotNull("redirect" to originalUrl, "errorKey" to errorKey))

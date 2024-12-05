# klite-oauth

Implements OAuth 2.0 login with several common [providers](src/OAuthClient.kt).

You need to provide implementations of [OAuthUser and OAuthUserProvider](src/OAuthUser.kt) in your project.

In the env/config, you need to provide clientId and secrets for each provider that you want to use:
```
GOOGLE_OAUTH_CLIENT_ID=...
GOOGLE_OAUTH_CLIENT_SECRET=...
```


```kotlin
context("/oauth") {
  register<OAuthUserProider>(MyUserProvider::class)
  register(httpClient())
  register<GoogleOAuthClient>()
  register<MicrosoftOAuthClient>()
  ...
  annotated<OAuthRoutes>()
}
```

Then navigate to e.g. `/oauth/google` or `/oauth/google?redirect=/return/path` to start authentication.

If you have only one OAuthClient registered, then you can use just `/oauth`.

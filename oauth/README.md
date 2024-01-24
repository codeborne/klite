# klite-oauth

Implements OAuth 2.0 login with several common [providers](src/OAuthClient.kt).

You need to provide implementations of [OAuthUser and OAuthUserProvider](src/OAuthUser.kt) in your project.

```kotlin
context("/oauth") {
  register<OAuthUserProider>(MyUserProvider::class)
  register<GoogleOAuthClient>()
  register<MicrosoftOAuthClient>()
  ...
  annotated<OAuthRoutes>()
}
```

Then navigate to e.g. `/oauth/google` or `/oauth/google?redirect=/return/path` to start authentication.

If you have only one OAuthClient registered, then you can use just `/oauth`.

# klite-oauth

Implements OAuth 2.0 login with several [providers](src/OAuthClient.kt).

You need to provide implementations of [OAuthUser and OAuthUserCreator](src/OAuthUser.kt).

```kotlin
context("/oauth") {
  register<OAuthUserCreator>(MyUserCreator::class)
  register<GoogleOAuthClient>()
  register<MicrosoftOAuthClient>()
  ...
  annotated<OAuthRoutes>()
}
```

Then navigate to e.g. `/oauth/google` or `/oauth/google?redirect=/return/path` to start authentication.

If you have only one OAuthClient registered, then you can use just `/oauth`.

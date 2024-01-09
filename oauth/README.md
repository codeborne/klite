# klite-oauth

Experimental helpers to implement OAuth

You need to provide implementations of [OAuthUser and OAuthUserRepository](src/OAuthUser.kt).

```kotlin
context("/oauth") {
  register<OAuthUserRepository>(UserRepository::class)
  annotated<OAuthRoutes>()
}
```

Then navigate to e.g. `/oauth/google` or `/oauth/google?redirect=/return/path` to start authentication.

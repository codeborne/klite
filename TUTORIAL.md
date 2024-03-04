# Klite Tutorial

This tutorial will guide you through the process of creating a TODO API backend appliation.

## Dependencies

Create a Kotlin Gradle project in IntelliJ IDEA, then add the Klite dependency to the `build.gradle.kts` file:

```kts
repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
}

dependencies {
  val kliteVersion = "paste latest version here"
  implementation("com.github.codeborne.klite:klite-server:$kliteVersion")
}
```

## Launcher

Good frameworks allow you to write your own main function, so that you are in control of the application lifecycle.
In Klite, you need to create an instance of [Server](server/src/klite/Server.kt):

```kotlin
import klite.Server

fun main() {
  Server().start()
}
```

This will start a server on the default port 8080.
You can change the listening IP and port by passing it as an argument to the `Server` constructor.

## Config

Most applications need some configuration. 12-factor applications use environment variables for configuration.
Klite provides a simple way to access environment variables using the [Config](core/src/Config.kt) object, and the Server already uses it to read the port such as `Config["PORT"]`, among other things.

In development, it makes sense to use an `.env` file to set environment variables, and also commit this file, so that fellow developers will get things running out of the box:

```kotlin
fun main() {
  Config.useEnvFile() // loads .env file if it exists, and uses values from file only if they are not set in the environment
  Server().start()
}
```

Klite also supports defining environment name with the ENV environment variable. Default values are `dev`, `test`, `prod`.

You may want to disable loading of `.env` file in production, and use environment variables only:

```kotlin
if (!Config.isProd) Config.useEnvFile()
```

Even if you don't add this `if`, Config still has precedence for environment variables over `.env` file.

## Routes

It's time to add our routes. Klite routes must be defined in a **context** (URL prefix with common meaning).

```kotlin
Server().apply {
  context("/api") {
    get("/hello") { "Hello, world!" }
  }
  start()
}
```

You can now get the plain text response from `http://localhost:8080/api/hello`.
This will also handle request logging for you.

## JSON

As is typical, we want our API to return JSON.

Klite uses registered [BodyRenderer](server/src/klite/Body.kt)s to render the response. By default, it uses `TextRenderer` to render plain text.

You can register more renderers to handle different content types.
Klite uses the `Accept` header to determine which content type the client wants.

Add [klite-json](json) dependency to your `build.gradle.kts`:

```kts
implementation("com.github.codeborne.klite:klite-json:$kliteVersion")
```

Note: [klite-jackson](jackson) and [klite-serialization](serialization) are alternatives, if you prefer more heavy-weight and harder to configure libraries.

Now you can register the `JsonBody` renderer and parser for the whole application:

```kotlin
  use<JsonBody>()
  context("/api") {
    get("/todos") { listOf(Todo("Buy groceries")) }
  }

  data class Todo(val item: String)
```

Or you may want to support only json within the `/api` context:

```kotlin
  context("/api") {
    useOnly<JsonBody>()
    ...
  }
```

You can now get the json response using `http://localhost:8080/api/todos`.

## HTML

If you want to render HTML server-side, you can implement your own `BodyRenderer` using your favorite template engine or just use Kotlin template strings for HTML generation.

```kotlin
  context("/html") {
    get("/:name") {
      send(OK, """
        <h1>Hello, ${+path("name")}!</h1>
      """, MimeTypes.html)
    }
  }
```

You can now get the json response using `http://localhost:8080/html/klite`.

Note the `+` before `path("name")`. This is a special operator to escape HTML characters in the string, import it from `klite.html.unaryPlus`.

## Assets / Static files

Instead, modern applications would use a frontend framework, such as Svelte or React, and serve the frontend as a static site.

Klite has a built-in static file server, which you can use to serve your frontend files.

```kotlin
  assets("/", AssetsHandler(Path.of("public"), useIndexForUnknownPaths = true))
```

This adds its own context, it is a good idea to use `/` as the path, so that files are available from shorter URLs.

The latter parameter is useful for SPA (single-page applications), where the frontend router handles the URL.

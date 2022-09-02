# klite-server

This is the main component of Klite - the [Server](src/klite/Server.kt).

Create an instance, overriding any defaults using named constructor parameters, add contexts and routes, then start.

Basic usage:
```kotlin
  Server().apply {
    context("/api") {
      // Lambda routes
      get("/hello") { "Hello World" }
      post("/hello") { "You posted: $rawBody" }

      // or take routes from annotated functions of a class (better for testing)
      annotated<MyRoutes>()
    }

    start()
  }
```
See the [sample subproject](../sample/src/Launcher.kt) for a full working example.

Route handlers run in context of [HttpExchange](src/klite/HttpExchange.kt) and can use its methods to work with
request and response.

Anything returned from a handler will be passed to [BodyRenderer](src/klite/Body.kt) to output the response with correct Content-Type. BodyRenderer is chosen based on the Accept request header or first one if no matches.

POST/PUT requests with body will be parsed using one of registered [BodyParsers](src/klite/Body.kt) according to the request Content-Type header; `text/plain` and `application/x-www-form-urlencoded` are enabled by default.

use<[JsonBody](../jackson/src/JsonBody.kt)>() for `application/json` support.

## Contexts

All routes must be organized into contexts with path prefixes. A context with the longest matching path prefix is chosen for handling a request.

## Assets

A simple [AssetsHandler](src/klite/AssetsHandler.kt) is provided to serve static resources.

```kotlin
assets("/", AssetsHandler(Path.of("public")))
```

For SPA client-side routing support, create AssetsHandler with `useIndexForUnknownPaths = true`.
*Warning:* this won't return 404 responses for missing paths anymore, but will render the index file.

## Config

[Config](src/klite/Config.kt) object is provided for an easy way to read System properties or env vars.

Use `Config.fromEnvFile()` if you want to load default config from an `.env`. This is useful for local development.

## Registry

[Registry](src/klite/Registry.kt) and it's default implementation - `DependencyInjectingRegistry` - provide
a simple way to register and require both Klite components and repositories/services of your application.

`DependencyInjectingRegistry` is used by default and can create any classes by recursively creating their constructor
arguments (dependencies).

## Decorators

You can add both global and context-specific [decorators](src/klite/Decorator.kt), including `Before` and `After` handlers.
The order is important, and decorators apply to all *following routes* that are defined in the same context.

## HTML templates for server-side rendering

No built-in support for that. You may either implement a [BodyRenderer](src/klite/Body.kt) that will pass route responses to your favorite template engine or just call the engine in your routes and produce html output directly with `send(OK, "html", "text/html")`.

In Kotlin, you may also consider using template strings for html/xml generation, see the provided [helpers](src/klite/html/Helpers.kt):
```kotlin
get("/hello") {
  """<html><body><h1>Hello ${+query("who")}</h1></body></html>"""
}
```

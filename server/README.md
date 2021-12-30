# klite-server

This is the main component of Klite - the [Server](src/klite/Server.kt).

Create and instance, overriding any defaults using named constructor parameters, add contexts and routes, then start.

Basic usage:
```kt
  Server().apply {
    context("/api") {
      // Lambda routes
      get("/hello") { "Hello World" }

      // or take routes from annotated functions of a class (better for testing)
      annotated<MyRoutes>()
    }

    start()
  }
```
See [sample subproject](../sample/src/Launcher.kt) for a full working example.

Route handlers run in context of [HttpExchange](src/klite/HttpExchange.kt) and can use its methods to work with
request and response.

Anything returned from a handler will be passed to [BodyRenderer](src/klite/Body.kt) to output the response with correct Content-Type. BodyRenderer is chosen based on the Accept request header or first one if no matches.

## Contexts

All routes must be organized into contexts with path prefixes. A context with the longest matching path prefix is chosen for handling a request.

## Assets

A simple [AssetsHandler](src/klite/AssetsHandler.kt) is provided to serve static resources.

```kt
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

You can add both global and context-specific [decorators](src/klite/Decorator.kt), including Before and After handlers.
The order is important, and decorators apply to all *following routes* that are defined in the same context.

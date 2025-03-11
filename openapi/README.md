# klite-openapi

A module to generate OpenAPI spec for Klite routes.

Input/output parameters, paths, names will be generated from annotated route classes automatically.
Use Swagger/OpenAPI annotations to specify descriptions or more details.

Usage:
```kotlin
  @OpenAPIDefinition(info = Info(title = "My API", version = "1.x"))
  class MyRoutes { ... }

  context("/api") {
    useOnly<JsonBody>()
    annotated<MyRoutes>()
    // ... more routes
    openApi(annotations = MyRoutes::class.annotations) // adds /openapi endpoint to the context
  }
```

See [sample Launcher](../sample/src/Launcher.kt).

## Swagger UI

You can request the following endpoints in your context (`/api` in this case):

* `/openapi.json` - will serve Open API json spec
* `/openapi.html` - will serve Swagger UI
* `/openapi` - will detect if you are asking for json or html

## Docs

* Spec: https://swagger.io/specification/
* Sample: https://github.com/OAI/OpenAPI-Specification/blob/main/examples/v3.0/api-with-examples.json

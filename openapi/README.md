# klite-openapi

An experimental module to generate OpenAPI spec for klite routes.

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
    openApi(annotations = MyRoutes::class.annotations) // adds /openapi endpoint to the /api context
  }
```

See [sample Launcher](../sample/src/Launcher.kt).

## Swagger UI

For nice visual representation of OpenAPI json output:
* add `before(CorsHandler())` before `openApi()`, as Swagger UI requires CORS to request openapi json from another host/domain
* `docker run -d -p 8080:8088 -e SWAGGER_JSON_URL= swaggerapi/swagger-ui`
* Open http://localhost:8088/?url=http://YOUR-IP:PORT/api/openapi
* Alternatively, use https://petstore.swagger.io/?url= if your `/openapi` route is available over https.

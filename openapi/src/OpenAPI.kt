package klite.openapi

import klite.HttpExchange
import klite.MimeTypes
import klite.RequestMethod.GET
import klite.Route
import klite.Router
import klite.StatusCode.Companion.OK
import org.intellij.lang.annotations.Language

/**
 * Adds an /openapi endpoints to the context, listing all the preceding routes.
 * This handler will try to gather all the information about request/parameters/response automatically, but you can use Swagger annotations to specify more details.
 * - Pass [@OpenAPIDefinition][io.swagger.v3.oas.annotations.OpenAPIDefinition] or only [@Info][io.swagger.v3.oas.annotations.info.Info] annotation to this function to specify general info
 * - Pass [@SecurityScheme][io.swagger.v3.oas.annotations.security.SecurityScheme] to define authorization
 * - Use [@Operation][io.swagger.v3.oas.annotations.Operation] annotation to describe each route
 * - Use [@SecurityRequirement][io.swagger.v3.oas.annotations.security.SecurityRequirement] annotations to reference security requirements of the route
 * - Use [@RequestBody][io.swagger.v3.oas.annotations.parameters.RequestBody] annotations to define the request body
 * - Use [@ApiResponse][io.swagger.v3.oas.annotations.responses.ApiResponse] annotations to define one or many possible responses
 * - Use [@Hidden][io.swagger.v3.oas.annotations.Hidden] to hide a route from the spec
 * - [@Parameter][io.swagger.v3.oas.annotations.Parameter] annotation can be used on method parameters directly
 * - [@Tag][io.swagger.v3.oas.annotations.tags.Tag] annotation is supported on route classes for grouping of routes
 */
fun Router.openApi(path: String = "/openapi", annotations: List<Annotation> = emptyList()) {
  add(Route(GET, "$path.json".toRegex(), annotations) { generateOpenAPI() })
  add(Route(GET, "$path.html".toRegex(), annotations) { swaggerUI(path) })
  add(Route(GET, path.toRegex(), annotations) {
    if (accept(MimeTypes.html)) swaggerUI(path)
    else generateOpenAPI()
  })
}

@Language("html")
private fun HttpExchange.swaggerUI(path: String) = send(OK, """
  <!DOCTYPE html>
  <html lang="en">
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="SwaggerUI">
    <title>SwaggerUI</title>
    <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist/swagger-ui.css">
  </head>
  <body>
    <div id="swagger-ui"></div>
    <script src="https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js" crossorigin></script>
    <script>
      onload = () => {
        ui = SwaggerUIBundle({
          url: '${path.substringAfter("/")}.json',
          dom_id: '#swagger-ui',
        })
      }
    </script>
  </body>
  </html>
""", MimeTypes.html)

package klite.openapi

import io.swagger.v3.oas.annotations.Operation
import klite.Router
import klite.StatusCode.Companion.OK
import klite.publicProperties
import klite.toValues
import klite.trimToNull

// Spec: https://swagger.io/specification/
// Sample: https://github.com/OAI/OpenAPI-Specification/blob/main/examples/v3.0/api-with-examples.json

/**
 * Adds an /openapi endpoint to the context, describing all the routes.
 * Use @Operation swagger annotations to describe the routes.
 */
fun Router.openApi(path: String = "/openapi", title: String = "API", version: String = "1.0.0") {
  get(path) {
    mapOf(
      "openapi" to "3.0.0",
      "info" to mapOf("title" to title, "version" to version),
      "servers" to listOf(mapOf("url" to fullUrl(prefix))),
      // TODO: need to transform template paths into OpenAPI format with {}
      // TODO: describe parameters from route methods
      "paths" to routes.groupBy { it.path.toString() }.mapValues { (_, routes) ->
        routes.associate {
          val op = it.annotation<Operation>()
          (op?.method?.trimToNull() ?: it.method.name).lowercase() to mapOf(
            "responses" to mapOf(
              OK.value to mapOf("description" to "OK")
            )
          ) + ((op?.toValues(op.publicProperties.filter { it.name != "method" }) ?: emptyMap()).filterValues {
            it != "" && it != false && (it as? Array<*>)?.isEmpty() != true
          })
        }
      }
    )
  }
}

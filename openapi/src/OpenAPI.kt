package klite.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.PathItem.HttpMethod
import klite.MimeTypes
import klite.Router
import klite.StatusCode.Companion.OK

fun Router.openApi(path: String = "/openapi") {
  get(path) {
    // TODO: publicProperties does not enumerate Java public properties, maybe file a bug to kotlin-reflect
    send(OK, OpenAPI().apply {
      routes.forEach {
        path(it.path.toString(), PathItem().apply {
          operation(HttpMethod.valueOf(it.method.toString()), Operation().apply {
            tags = listOf(it.handler::class.simpleName)
            summary = it.handler::class.simpleName
            description = it.handler::class.simpleName
          })
        })
      }
    }.toString(), MimeTypes.text)
  }
}

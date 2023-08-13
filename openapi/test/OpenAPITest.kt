package klite.openapi

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY
import io.swagger.v3.oas.annotations.responses.ApiResponse
import klite.RequestMethod.POST
import klite.Route
import klite.StatusCode.Companion.OK
import org.junit.jupiter.api.Test

class OpenAPITest {
  @Test fun `anonymous route`() {
    expect(toOperation(Route(POST, "/x".toRegex()) {})).toEqual("post" to mapOf(
      "operationId" to null,
      "parameters" to null,
      "requestBody" to null,
      "responses" to mapOf(OK.value to mapOf("description" to "OK"))
    ))
  }

  @Test fun `anonymous route with annotation`() {
    val route = Route(POST, "/x".toRegex()) @Operation(
      operationId = "opId",
      summary = "summary",
      parameters = [Parameter(name = "param", description = "description", `in` = QUERY)],
      responses = [ApiResponse(description = "desc", responseCode = "302")]
    ) {}
    expect(toOperation(route)).toEqual("post" to mapOf(
      "operationId" to "opId",
      "parameters" to listOf(mapOf("name" to "param", "description" to "description", "in" to "query")),
      "requestBody" to null,
      "responses" to mapOf("302" to mapOf("description" to "desc"))
    ))
  }
}

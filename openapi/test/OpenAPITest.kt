package klite.openapi

import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.PATH
import io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import klite.RequestMethod.GET
import klite.RequestMethod.POST
import klite.Route
import klite.StatusCode.Companion.OK
import klite.annotations.FunHandler
import klite.annotations.PathParam
import klite.annotations.QueryParam
import klite.annotations.annotation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class OpenAPITest {
  @Test fun nonEmptyValues() {
    @Tag(name = "hello") class Dummy {}
    expect(Dummy::class.annotation<Tag>()!!.toNonEmptyValues()).toEqual(mapOf("name" to "hello"))
  }

  @Test fun `route classes to tags`() {
    expect(toTags(listOf(Route(GET, "/hello".toRegex(), handler = FunHandler(this, OpenAPITest::toString)))))
      .toContainExactly(mapOf("name" to "OpenAPITest"))
  }

  @Test fun `route classes to tags with Tag annotation`() {
    @Tag(name = "my-tag", description = "My description") class MyRoutes {}
    expect(toTags(listOf(Route(GET, "/hello".toRegex(), handler = FunHandler(MyRoutes(), MyRoutes::toString)))))
      .toContainExactly(mapOf("name" to "my-tag", "description" to "My description"))
  }

  @Test fun `annotated route`() {
    class MyRoutes {
      fun getUser(@PathParam userId: UUID, @QueryParam force: Boolean = false, @QueryParam date: LocalDate, @QueryParam simpleString: String?) = null
    }
    expect(toOperation(Route(POST, "/x".toRegex(), handler = FunHandler(MyRoutes(), MyRoutes::getUser)))).toEqual("post" to mapOf(
      "operationId" to "MyRoutes.getUser",
      "tags" to listOf("MyRoutes"),
      "parameters" to listOf(
        mapOf("name" to "userId", "required" to true, "in" to PATH, "schema" to mapOf("type" to "string", "format" to "uuid")),
        mapOf("name" to "force", "required" to false, "in" to QUERY, "schema" to mapOf("type" to "boolean")),
        mapOf("name" to "date", "required" to true, "in" to QUERY, "schema" to mapOf("type" to "string", "format" to "date")),
        mapOf("name" to "simpleString", "required" to false, "in" to QUERY, "schema" to mapOf("type" to "string"))
      ),
      "requestBody" to null,
      "responses" to mapOf(OK.value to mapOf("description" to "OK"))
    ))
  }

  @Test fun `anonymous route`() {
    expect(toOperation(Route(POST, "/x".toRegex()) {})).toEqual("post" to mapOf(
      "operationId" to null,
      "tags" to emptyList<Any>(),
      "parameters" to null,
      "requestBody" to null,
      "responses" to mapOf(OK.value to mapOf("description" to "OK"))
    ))
  }

  @Test @Disabled fun `anonymous route with annotation`() {
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

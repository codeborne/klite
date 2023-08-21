package klite.openapi

import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import klite.HttpExchange
import klite.MimeTypes
import klite.RequestMethod.GET
import klite.RequestMethod.POST
import klite.Route
import klite.StatusCode.Companion.NoContent
import klite.StatusCode.Companion.OK
import klite.annotations.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
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

  @Test fun `annotated klite route`() {
    class MyRoutes {
      fun saveUser(@PathParam userId: UUID, @QueryParam force: Boolean = false) = null
    }
    expect(toOperation(Route(POST, "/x".toRegex(), handler = FunHandler(MyRoutes(), MyRoutes::saveUser)))).toEqual("post" to mapOf(
      "operationId" to "MyRoutes.saveUser",
      "tags" to listOf("MyRoutes"),
      "parameters" to listOf(
        mapOf("name" to "userId", "required" to true, "in" to PATH, "schema" to mapOf("type" to "string", "format" to "uuid")),
        mapOf("name" to "force", "required" to false, "in" to QUERY, "schema" to mapOf("type" to "boolean"))
      ),
      "requestBody" to null,
      "responses" to mapOf(OK.value to mapOf("description" to "OK", "content" to mapOf(MimeTypes.json to mapOf("schema" to mapOf("type" to "null")))))
    ))
  }

  @Test fun parameters() {
    class MyRoutes {
      fun withParams(@PathParam date: LocalDate, @QueryParam simpleString: String?, @CookieParam dow: DayOfWeek) = null
    }
    expect(FunHandler(MyRoutes(), MyRoutes::withParams).params.filter { it.source != null }.map { toParameter(it) }).toContainExactly(
      mapOf("name" to "date", "required" to true, "in" to PATH, "schema" to mapOf("type" to "string", "format" to "date")),
      mapOf("name" to "simpleString", "required" to false, "in" to QUERY, "schema" to mapOf("type" to "string")),
      mapOf("name" to "dow", "required" to true, "in" to COOKIE, "schema" to mapOf("type" to "string", "enum" to DayOfWeek.values().toList()))
    )
  }

  @Test fun `request body`() {
    data class User(val name: String, val id: UUID)
    class MyRoutes {
      fun saveUser(e: HttpExchange, @PathParam userId: UUID, body: User) {}
    }
    expect(toOperation(Route(POST, "/x".toRegex(), handler = FunHandler(MyRoutes(), MyRoutes::saveUser)))).toEqual("post" to mapOf(
      "operationId" to "MyRoutes.saveUser",
      "tags" to listOf("MyRoutes"),
      "parameters" to listOf(
        mapOf("name" to "userId", "required" to true, "in" to PATH, "schema" to mapOf("type" to "string", "format" to "uuid"))
      ),
      "requestBody" to mapOf("content" to
        mapOf(MimeTypes.json to mapOf(
          "schema" to mapOf(
            "type" to "object",
            "properties" to mapOf(
              "name" to mapOf("type" to "string"),
              "id" to mapOf("type" to "string", "format" to "uuid")
            ),
            "required" to setOf("name", "id")
          )
        ))
      ),
      "responses" to mapOf(NoContent.value to mapOf("description" to "No content"))
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

  @Test fun `anonymous route with annotation`() {
    val route = Route(POST, "/x".toRegex()) @Operation(
      operationId = "opId",
      tags = ["my-tag"],
      summary = "summary",
      parameters = [Parameter(name = "param", description = "description", `in` = QUERY)],
      responses = [ApiResponse(description = "desc", responseCode = "302")]
    ) {}
    expect(toOperation(route)).toEqual("post" to mapOf(
      "operationId" to "opId",
      "tags" to listOf("my-tag"),
      "parameters" to listOf(mapOf("name" to "param", "in" to QUERY, "description" to "description")),
      "requestBody" to null,
      "responses" to mapOf("302" to mapOf("description" to "desc")),
      "summary" to "summary"
    ))
  }
}

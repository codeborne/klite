package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.mockk
import klite.StatusCode.Companion.BadRequest
import klite.StatusCode.Companion.Forbidden
import klite.StatusCode.Companion.InternalServerError
import klite.StatusCode.Companion.NotFound
import org.junit.jupiter.api.Test

class ErrorHandlerTest {
  val exchange = mockk<HttpExchange>()
  val errorHandler = ErrorHandler()

  @Test fun `status code exception`() {
    expect(errorHandler.toResponse(exchange, ForbiddenException("Some message")))
      .toEqual(ErrorResponse(Forbidden, "Some message"))
  }

  @Test fun `required parameter exception`() {
    expect(errorHandler.toResponse(exchange, NullPointerException("Parameter specified as non-null is null, parameter hello")))
      .toEqual(ErrorResponse(BadRequest, "hello is required"))
  }

  @Test fun `no such element`() {
    expect(errorHandler.toResponse(exchange, NoSuchElementException("List is empty.")))
      .toEqual(ErrorResponse(NotFound, "List is empty."))
  }

  @Test fun unhandled() {
    expect(errorHandler.toResponse(exchange, Exception("Kaboom")))
      .toEqual(ErrorResponse(InternalServerError, "Kaboom"))
  }
}

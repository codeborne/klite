package klite

open class StatusCodeException(val statusCode: StatusCode, content: String? = null): Exception(content) {
  override fun fillInStackTrace() = this
}

class NotFoundException(content: String? = null): StatusCodeException(StatusCode.NotFound, content)
class BadRequestException(content: String? = null): StatusCodeException(StatusCode.BadRequest, content)
class ForbiddenException(content: String? = null): StatusCodeException(StatusCode.Forbidden, content)
class UnauthorizedException(content: String? = null): StatusCodeException(StatusCode.Unauthorized, content)
class UnsupportedMediaTypeException(contentType: String?): StatusCodeException(StatusCode.UnsupportedMediaType, "Unsupported Content-Type: $contentType")
class NotAcceptableException(contentType: String?): StatusCodeException(StatusCode.NotAcceptable, "Unsupported Accept: $contentType")

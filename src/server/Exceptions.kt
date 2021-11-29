package server

open class StatusCodeException(val statusCode: Int, content: String): Exception(content)
class NotFoundException(content: String): StatusCodeException(404, content)
class ForbiddenException(content: String): StatusCodeException(403, content)
class UnauthorizedException(content: String): StatusCodeException(401, content)

package klite.jdbc

open class BusinessException(messageKey: String, cause: Throwable? = null): Exception(messageKey, cause)

class AlreadyExistsException(cause: Throwable? = null): BusinessException("errors.alreadyExists", cause)

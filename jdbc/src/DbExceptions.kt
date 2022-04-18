package klite.jdbc

import klite.BusinessException

class AlreadyExistsException(cause: Throwable? = null): BusinessException("errors.alreadyExists", cause)

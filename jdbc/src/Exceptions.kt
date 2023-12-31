package klite.jdbc

import klite.BusinessException

class AlreadyExistsException(cause: Throwable? = null): BusinessException(cause?.message?.let {
  val pos = it.indexOf("Detail: ")
  if (pos >= 0) it.substring(pos + 8) else null
} ?: "errors.alreadyExists", cause)

class StaleEntityException: BusinessException("errors.staleEntity")

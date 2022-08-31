package klite.jdbc

import java.sql.Statement
import kotlin.reflect.KClass

class GeneratedKey<T: Any>(val convertTo: KClass<T>? = null) {
  lateinit var value: T
}

@Suppress("UNCHECKED_CAST")
internal fun Statement.processGeneratedKeys(values: Map<String, *>) =
  generatedKeys.map {
    values.forEach { e ->
      (e.value as? GeneratedKey<Any>)?.let {
        val value = if (it.convertTo != null) getString(e.key) else getObject(e.key)
        it.value = JdbcConverter.from(value, it.convertTo) as Any
      }
    }
  }

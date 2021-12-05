package klite.annotations

import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private typealias Creator<T> = (s: String) -> T

open class TypeConverter(moreCreators: Map<KClass<*>, Creator<*>> = emptyMap()) {
  private val creators = ConcurrentHashMap(mapOf(
    UUID::class to UUID::fromString
  ) + moreCreators)

  inline fun <reified T: Any> fromString(s: String) = fromString(s, T::class)

  @Suppress("UNCHECKED_CAST")
  fun <T: Any> fromString(s: String, type: KClass<T>): T =
    (creators[type] as? Creator<T> ?: findCreator(type).also { creators[type] = it }).invoke(s)

  private fun <T: Any> findCreator(type: KClass<T>): Creator<T> =
    try { constructorCreator(type) }
    catch (e: NoSuchMethodException) {
      try { parseMethodCreator(type) }
      catch (e2: NoSuchMethodException) {
        error("Cannot create instances of $type:\n$e\n$e2")
      }
    }

  private fun <T: Any> constructorCreator(type: KClass<T>): Creator<T> {
    val constructor = type.javaObjectType.getConstructor(String::class.java)
    return { s: String ->
      try { constructor.newInstance(s) }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }

  private fun <T: Any> parseMethodCreator(type: KClass<T>): Creator<T> {
    val parse = type.java.getMethod("parse", CharSequence::class.java)
    return { s: String ->
      try { parse.invoke(null, s) as T }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }
}

package klite

import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

typealias StringConverter<T> = (s: String) -> T

object Converter {
  private val converters: MutableMap<KClass<*>, StringConverter<*>> = ConcurrentHashMap(mapOf(
    UUID::class to UUID::fromString,
    Currency::class to Currency::getInstance
  ))
  operator fun <T: Any> set(type: KClass<T>, converter: StringConverter<T>) { converters[type] = converter }

  fun supports(type: KClass<*>) = converters[type] != null

  inline fun <reified T: Any> fromString(s: String) = fromString(s, T::class)

  @Suppress("UNCHECKED_CAST")
  fun <T: Any> fromString(s: String, type: KClass<T>): T =
    (converters[type] as? StringConverter<T> ?: findCreator(type).also { converters[type] = it }).invoke(s)

  private fun <T: Any> findCreator(type: KClass<T>): StringConverter<T> =
    if (type.isSubclassOf(Enum::class)) enumCreator(type) else
    try { constructorCreator(type) }
    catch (e: NoSuchMethodException) {
      try { parseMethodCreator(type) }
      catch (e2: NoSuchMethodException) {
        error("Don't know how to convert String to $type:\n$e\n$e2")
      }
    }

  private fun <T: Any> enumCreator(type: KClass<T>): StringConverter<T> {
    val enumConstants = type.java.enumConstants
    return { s -> enumConstants.find { (it as Enum<*>).name == s } ?: error("No $type constant: $s") }
  }

  private fun <T: Any> constructorCreator(type: KClass<T>): StringConverter<T> {
    val constructor = type.javaObjectType.getConstructor(String::class.java)
    return { s ->
      try { constructor.newInstance(s) }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }

  private fun <T: Any> parseMethodCreator(type: KClass<T>): StringConverter<T> {
    val parse = type.java.getMethod("parse", CharSequence::class.java)
    return { s ->
      try { parse.invoke(null, s) as T }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }
}

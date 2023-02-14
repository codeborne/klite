package klite

import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

typealias FromStringConverter<T> = (s: String) -> T

object Converter {
  private val converters: MutableMap<KClass<*>, FromStringConverter<*>> = ConcurrentHashMap(mapOf(
    UUID::class to UUID::fromString,
    Currency::class to Currency::getInstance,
    Locale::class to { Locale.forLanguageTag(it.replace('_', '-')) }
  ))

  operator fun <T: Any> set(type: KClass<T>, converter: FromStringConverter<T>) { converters[type] = converter }
  inline fun <reified T: Any> use(noinline converter: FromStringConverter<T>) = set(T::class, converter)

  fun supports(type: KClass<*>) = converters[type] != null
  fun forEach(block: (type: KClass<*>, converter: FromStringConverter<*>) -> Unit) = converters.forEach { block(it.key, it.value) }

  inline fun <reified T: Any> from(s: String) = from(s, T::class)

  @Suppress("UNCHECKED_CAST") // TODO: really support for KType in Converter
  fun <T: Any> from(s: String, type: KType): T = from(s, type.classifier as KClass<T>)

  @Suppress("UNCHECKED_CAST")
  fun <T: Any> from(s: String, type: KClass<T>): T =
    (converters[type] as? FromStringConverter<T> ?: findCreator(type).also { converters[type] = it }).invoke(s)

  private fun <T: Any> findCreator(type: KClass<T>): FromStringConverter<T> =
    if (type.isSubclassOf(Enum::class)) enumCreator(type) else
    try { constructorCreator(type) }
    catch (e: NoSuchMethodException) {
      try { parseMethodCreator(type) }
      catch (e2: NoSuchMethodException) {
        error("Don't know how to convert String to $type:\n$e\n$e2")
      }
    }

  private fun <T: Any> enumCreator(type: KClass<T>): FromStringConverter<T> {
    val enumConstants = type.java.enumConstants
    return { s -> enumConstants.find { (it as Enum<*>).name == s } ?: error("No $type constant: $s") }
  }

  private fun <T: Any> constructorCreator(type: KClass<T>): FromStringConverter<T> {
    val constructor = type.javaObjectType.getDeclaredConstructor(String::class.java)
    if (type.hasAnnotation<JvmInline>()) constructor.isAccessible = true
    return { s ->
      try { constructor.newInstance(s) }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T: Any> parseMethodCreator(type: KClass<T>): FromStringConverter<T> {
    val parse = type.java.getMethod("parse", CharSequence::class.java)
    return { s ->
      try { parse.invoke(null, s) as T }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }
}

val KType.java get() = (classifier as KClass<*>).java

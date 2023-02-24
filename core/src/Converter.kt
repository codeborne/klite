package klite

import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

typealias FromStringConverter<T> = (s: String) -> T

private class NoConverter<T: Any>(val type: KClass<T>): FromStringConverter<T> {
  override fun invoke(s: String) = error("No known converter from String to $type")
}

/**
 * Converts Strings to value types.
 * Supports enums, constructor invocation, parse() methods (e.g. java.time).
 * Can register custom converters with `use` method.
 */
@Suppress("UNCHECKED_CAST")
object Converter {
  private val log = logger()
  private val converters: MutableMap<KClass<*>, FromStringConverter<*>> = ConcurrentHashMap(mapOf(
    UUID::class to UUID::fromString,
    Currency::class to Currency::getInstance,
    Locale::class to { Locale.forLanguageTag(it.replace('_', '-')) }
  ))

  operator fun <T: Any> set(type: KClass<T>, converter: FromStringConverter<T>) { converters[type] = converter }
  inline fun <reified T: Any> use(noinline converter: FromStringConverter<T>) = set(T::class, converter)

  fun supports(type: KClass<*>) = of(type) !is NoConverter
  fun forEach(block: (type: KClass<*>, converter: FromStringConverter<*>) -> Unit) = converters.forEach { block(it.key, it.value) }

  // TODO: really support for KType in Converter
  fun <T: Any> from(s: String, type: KType): T = from(s, type.classifier as KClass<T>)
  fun <T: Any> from(s: String, type: KClass<T>): T = of(type).invoke(s)
  fun from(o: Any?, type: KType): Any? = if (o is String) from(o, type) else o
  inline fun <reified T: Any> from(s: String) = from(s, T::class)

  internal fun <T: Any> of(type: KClass<T>) =
    converters[type] as? FromStringConverter<T> ?: (findCreator(type) ?: NoConverter(type)).also { set(type, it) }

  private fun <T: Any> findCreator(type: KClass<T>): FromStringConverter<T>? =
    if (type.isSubclassOf(Enum::class)) enumCreator(type) else
    try { constructorCreator(type) }
    catch (e: NoSuchMethodException) {
      try { parseMethodCreator(type) }
      catch (e2: NoSuchMethodException) {
        log.warn("Cannot find a way to convert String to $type\n$e\n$e2")
        null
      }
    }

  private fun <T: Any> enumCreator(type: KClass<T>): FromStringConverter<T> {
    val enumConstants = type.java.enumConstants
    return { s -> enumConstants.find { (it as Enum<*>).name == s } ?: error("No $type constant: $s") }
  }

  private fun <T: Any> constructorCreator(type: KClass<T>): FromStringConverter<T> {
    val constructor = type.javaObjectType.getDeclaredConstructor(String::class.java)
    if (type.isValue && type.hasAnnotation<JvmInline>()) constructor.isAccessible = true
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

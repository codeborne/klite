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
  override fun invoke(s: String) = error("No known converter from String to $type, register with Converter.use()" +
    if (type.isData) " or implement matching toString() for a data class" else "")
}

/**
 * Converts Strings to value types.
 * Supports enums, constructor invocation, parse() methods (e.g. java.time).
 * Can register custom converters with `use` method.
 * Note: @JvmInline classes are converted by default, but Kotlin data are not; register them with `use` explicitly if needed.
 */
@Suppress("UNCHECKED_CAST")
object Converter {
  private val converters: MutableMap<KClass<*>, FromStringConverter<*>> = ConcurrentHashMap(mapOf(
    Any::class to Any::toString,
    CharSequence::class to Any::toString,
    UUID::class to UUID::fromString,
    Currency::class to Currency::getInstance,
    Locale::class to { Locale.forLanguageTag(it.replace('_', '-')) }
  ))

  operator fun <T: Any> set(type: KClass<T>, converter: FromStringConverter<T>) { converters[type] = converter }
  inline fun <reified T: Any> use(noinline converter: FromStringConverter<T>) = set(T::class, converter)

  fun supports(type: KClass<*>) = of(type) !is NoConverter
  fun forEach(block: (type: KClass<*>, converter: FromStringConverter<*>) -> Unit) = converters.forEach { block(it.key, it.value) }

  fun <T: Any> from(s: String, type: KType): T = (type.classifier as? KClass<T>)?.let { from(s, it) } ?: s as T
  fun <T: Any> from(s: String, type: KClass<T>): T = of(type, s).invoke(s)
  fun from(o: Any?, type: KType): Any? = if (o is String) from(o, type) else o
  inline fun <reified T: Any> from(s: String) = from(s, T::class)

  internal fun <T: Any> of(type: KClass<T>, sample: String? = null) =
    converters[type] as? FromStringConverter<T> ?: (findCreator(type, sample) ?: NoConverter(type)).also { set(type, it) }

  private fun <T: Any> findCreator(type: KClass<T>, sample: String?): FromStringConverter<T>? =
    if (type.isSubclassOf(Enum::class)) enumCreator(type) else
    try { constructorCreator(type, sample) }
    catch (e: NoSuchMethodException) {
      try { parseMethodCreator(type) }
      catch (e2: NoSuchMethodException) { null }
    }

  private fun <T: Any> enumCreator(type: KClass<T>): FromStringConverter<T> {
    val enumConstants = type.java.enumConstants.associateBy { (it as Enum<*>).name.uppercase() }
    return { s -> enumConstants[s.uppercase()] ?: error("No $type constant: $s") }
  }

  private fun <T: Any> constructorCreator(type: KClass<T>, sample: String?): FromStringConverter<T>? {
    val constructor = type.javaObjectType.getDeclaredConstructor(String::class.java)
    if (type.isData && constructor.newInstance(sample).toString() != sample) return null
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

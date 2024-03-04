package klite

import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

typealias FromStringConverter<T> = (s: String) -> T

private class NoConverter<T: Any>(val type: KClass<T>): FromStringConverter<T> {
  override fun invoke(s: String) = error("No known converter from String to $type, register with Converter.use()")
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
    Locale::class to { Locale.forLanguageTag(it.replace('_', '-')) }
  ))

  operator fun <T: Any> set(type: KClass<T>, converter: FromStringConverter<T>) { converters[type] = converter }
  inline fun <reified T: Any> use(noinline converter: FromStringConverter<T>) = set(T::class, converter)

  fun supports(type: KClass<*>) = of(type) !is NoConverter
  fun forEach(block: (type: KClass<*>, converter: FromStringConverter<*>) -> Unit) = converters.forEach { block(it.key, it.value) }

  fun <T: Any> from(s: String, type: KType): T = (type.classifier as? KClass<T>)?.let { from(s, it) } ?: s as T
  fun <T: Any> from(s: String, type: KClass<T>): T = of(type).invoke(s)
  fun from(o: Any?, type: KType): Any? = if (o is String) from(o, type) else o
  inline fun <reified T: Any> from(s: String) = from(s, T::class)

  internal fun <T: Any> of(type: KClass<T>) =
    (converters[type] ?: forceInitAndCheckAgain(type)) as FromStringConverter<T>? ?:
    (findCreator(type) ?: NoConverter(type)).also { set(type, it) }

  private fun <T: Any> forceInitAndCheckAgain(type: KClass<T>) =
    type.companionObjectInstance.run { converters[type] }

  private fun <T: Any> findCreator(type: KClass<T>): FromStringConverter<T>? =
    if (type.isData) null else
    if (type.isSubclassOf(Enum::class)) enumCreator(type) else
    try { javaConstructorCreator(type) }
    catch (e: NoSuchMethodException) {
      try { staticMethodCreator(type) }
      catch (e2: NoSuchMethodException) {
        try { kotlinConstructorCreator(type) }
        catch (e3: Exception) { null }
      }
    }

  private fun <T: Any> enumCreator(type: KClass<T>): FromStringConverter<T> {
    val enumConstants = type.java.enumConstants.associateBy { (it as Enum<*>).name.uppercase() }
    return { s -> enumConstants[s.uppercase()] ?: error("No $type constant: $s") }
  }

  private fun <T: Any> javaConstructorCreator(type: KClass<T>): FromStringConverter<T> {
    val constructor = type.javaObjectType.getDeclaredConstructor(String::class.java)
    if (type.isValue && type.hasAnnotation<JvmInline>()) constructor.isAccessible = true
    return { s ->
      try { constructor.newInstance(s) }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }

  private fun <T: Any> kotlinConstructorCreator(type: KClass<T>): FromStringConverter<T>? {
    val constructor = type.constructors.find { it.parameters.size == 1 && it.parameters.first().type.classifier == String::class } ?: return null
    return { s ->
      try { constructor.call(s) }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T: Any> staticMethodCreator(type: KClass<T>): FromStringConverter<T> {
    val parse = type.java.declaredMethods.find { it.returnType == type.java && it.parameterCount == 1 && it.parameterTypes[0].isAssignableFrom(String::class.java) } ?: throw NoSuchMethodException()
    return { s ->
      try { parse.invoke(null, s) as T }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }
}

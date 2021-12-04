package klite

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

interface Registry {
  fun <T: Any> require(type: KClass<T>): T
  fun <T: Any> requireAll(type: KClass<T>): List<T>
}

interface MutableRegistry: Registry {
  fun <T: Any> register(type: KClass<T>, instance: T)
}

inline fun <reified T: Any> Registry.require() = require(T::class)
inline fun <reified T: Any> Registry.requireAll() = requireAll(T::class)
inline fun <reified T: Any> MutableRegistry.register(instance: T) = register(T::class, instance)

@Suppress("UNCHECKED_CAST")
open class SimpleRegistry: MutableRegistry {
  private val instances = mutableMapOf<KClass<*>, Any>(Registry::class to this)

  override fun <T : Any> register(type: KClass<T>, instance: T) { instances[type] = instance }
  override fun <T: Any> require(type: KClass<T>) = instances[type] as? T ?: notFound(type)
  protected open fun <T: Any> notFound(type: KClass<T>): T = throw RegistryException("No registered instance of $type")
  override fun <T : Any> requireAll(type: KClass<T>): List<T> = instances.values.filter { it::class.isSubclassOf(type) } as List<T>
}

class RegistryException(message: String): Exception(message)

open class AutoCreatingRegistry: SimpleRegistry() {
  private val logger = logger()

  override fun <T: Any> notFound(type: KClass<T>): T = create(type).also { register(type, it) }

  open fun <T: Any> create(type: KClass<T>): T {
    val constructor = type.primaryConstructor ?: type.constructors.minByOrNull { it.parameters.size } ?: throw RegistryException("$type has no usable constructor")
    try {
      val args = constructor.parameters.filter { !it.isOptional }.associateWith { require(it.type.classifier as KClass<*>) }
      return constructor.callBy(args).also {
        logger.info("${type.simpleName}${args.values.map {it::class.simpleName}}")
      }
    } catch (e: Exception) {
      throw RegistryException("Failed to auto-create ${type.simpleName} with dependencies on ${constructor.parameters.map {it.type}}: ${e.message}")
    }
  }
}

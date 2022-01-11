package klite

import java.lang.System.Logger.Level.DEBUG
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

interface Registry {
  fun <T: Any> require(type: KClass<T>): T
  fun <T: Any> requireAll(type: KClass<T>): List<T>
}

interface MutableRegistry: Registry {
  fun <T: Any> register(type: KClass<T>)
  fun <T: Any, I: T> register(type: KClass<T>, implementation: KClass<I>)
  fun <T: Any> register(type: KClass<T>, instance: T)
}

inline fun <reified T: Any> Registry.require() = require(T::class)
inline fun <reified T: Any> Registry.requireAll() = requireAll(T::class)
inline fun <reified T: Any> MutableRegistry.register(instance: T) = register(T::class, instance)
inline fun <reified T: Any> MutableRegistry.register() = register(T::class)

class RegistryException(message: String, cause: Throwable? = null): Exception(message, cause)

@Suppress("UNCHECKED_CAST")
open class SimpleRegistry: MutableRegistry {
  private val instances = mutableMapOf<KClass<*>, Any>(Registry::class to this)

  override fun <T : Any> register(type: KClass<T>, instance: T) { instances[type] = instance }
  override fun <T : Any, I: T> register(type: KClass<T>, implementation: KClass<I>) = register(type, create(implementation))
  override fun <T : Any> register(type: KClass<T>) = register(type, create(type))

  override fun <T: Any> require(type: KClass<T>) = instances[type] as? T ?: create(type).also { register(type, it) }
  override fun <T : Any> requireAll(type: KClass<T>): List<T> = instances.values.filter { type.java.isAssignableFrom(it.javaClass) } as List<T>

  protected open fun <T: Any> create(type: KClass<T>): T = type.createInstance()
}

/**
 * Implements simple constructor injection that can easily replace your more complex dependency injection framework.
 * You may extend this class to override how exactly constructor parameters are created.
 */
open class DependencyInjectingRegistry: SimpleRegistry() {
  private val logger = logger()

  override fun <T: Any> create(type: KClass<T>): T {
    val constructor = chooseConstructor(type) ?: throw RegistryException("$type has no usable constructor")
    try {
      val args = createArgs(constructor)
      return constructor.callBy(args).also {
        logger.log(DEBUG) { "Auto-created ${type.simpleName}${args.values.map { it::class.simpleName }}" }
      }
    } catch (e: Exception) {
      throw RegistryException("Failed to auto-create ${type.simpleName} with dependencies on ${constructor.parameters.map {it.type}}", e.cause ?: e)
    }
  }

  protected open fun <T : Any> chooseConstructor(type: KClass<T>) =
    type.primaryConstructor ?: type.constructors.minByOrNull { it.parameters.size }

  protected open fun <T : Any> createArgs(constructor: KFunction<T>) =
    constructor.parameters.filter { !it.isOptional }.associateWith { require(it.type.classifier as KClass<*>) }
}

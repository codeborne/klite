package klite

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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
  override fun <T: Any> require(type: KClass<T>) = instances[type] as? T ?: error("No registered instance of $type")
  override fun <T : Any> requireAll(type: KClass<T>): List<T> = instances.values.filter { it::class.isSubclassOf(type) } as List<T>
}

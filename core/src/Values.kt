package klite

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

fun <T: Any> Map<String, Any?>.fromValues(type: KClass<T>, getValue: (KParameter) -> Any? = { Converter.from(get(it.name), it.type) }): T {
  val constructor = type.primaryConstructor!!
  val args = constructor.parameters.associateWith { getValue(it) }.filterNot { it.key.isOptional && it.value == null }
  return try {
    constructor.callBy(args)
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException("Cannot create $type using " + args.mapKeys { it.key.name }, e)
  }
}

package klite.json

import klite.Converter
import klite.publicProperties
import org.intellij.lang.annotations.Language
import java.io.PrintStream
import java.lang.System.err
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.io.path.PathWalkOption.INCLUDE_DIRECTORIES
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

/** Converts project data/enum/inline classes to TypeScript for front-end type-safety */
open class TSGenerator(
  private val customTypes: Map<String, String> = emptyMap(),
  private val typePrefix: String = "export "
) {
  @OptIn(ExperimentalPathApi::class)
  open fun generate(dir: Path, out: PrintStream = System.out) = dir.walk(INCLUDE_DIRECTORIES).filter { it.extension == "class" }.forEach {
    val className = dir.relativize(it).toString().removeSuffix(".class").replace("/", ".")
    try {
      val cls = Class.forName(className).kotlin
      render(cls)?.let {
        out.println("// $cls")
        out.println(typePrefix + it)
      }
    } catch (ignore: UnsupportedOperationException) {
    } catch (e: Exception) {
      err.println("// $className: $e")
    }
  }

  @Language("TypeScript") open fun render(cls: KClass<*>) =
    if (cls.isData || cls.java.isInterface && !cls.java.isAnnotation) renderInterface(cls)
    else if (cls.isValue) renderInline(cls)
    else if (cls.isSubclassOf(Enum::class)) renderEnum(cls)
    else null

  protected open fun renderEnum(cls: KClass<*>) = "enum " + tsName(cls) + " {" + cls.java.enumConstants.joinToString { "$it = '$it'" } + "}"

  protected open fun renderInline(cls: KClass<*>) = "type " + tsName(cls) + typeParams(cls) +
    " = " + tsType(cls.memberProperties.first().returnType)

  protected open fun typeParams(cls: KClass<*>) =
    cls.typeParameters.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") ?: ""

  @Suppress("UNCHECKED_CAST")
  protected open fun renderInterface(cls: KClass<*>): String? = StringBuilder().apply {
    val props = (cls.publicProperties as Sequence<KProperty1<Any, *>>).notIgnored.iterator()
    if (!props.hasNext()) return null
    append("interface ").append(tsName(cls)).append(typeParams(cls)).append(" {")
    props.iterator().forEach { p ->
      append(p.jsonName)
      if (p.returnType.isMarkedNullable) append("?")
      append(": ").append(tsType(p.returnType)).append("; ")
    }
    if (endsWith("; ")) setLength(length - 2)
    append("}")
  }.toString()

  protected open fun tsType(type: KType?): String {
    val cls = type?.classifier as? KClass<*>
    val ts = customTypes[type.toString()] ?: when {
      cls == null -> "any"
      cls.isValue -> tsName(cls)
      cls.isSubclassOf(Enum::class) -> tsName(cls)
      cls.isSubclassOf(Boolean::class) -> "boolean"
      cls.isSubclassOf(Number::class) -> "number"
      cls.isSubclassOf(Iterable::class) || cls.java.isArray -> "Array"
      cls.isSubclassOf(Map::class) -> "Record"
      cls.isSubclassOf(CharSequence::class) || Converter.supports(cls) -> "string"
      cls == KProperty1::class -> "keyof " + tsType(type.arguments.first().type)
      cls.isData || cls.java.isInterface -> tsName(cls)
      else -> "any"
    }
    return if (ts == "any" || ts.startsWith("keyof ")) ts
    else ts + (type?.arguments?.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") { tsType(it.type) } ?: "")
  }

  protected open fun tsName(type: KClass<*>) = type.java.name.substringAfterLast(".").replace("$", "")

  companion object {
    @JvmStatic fun main(args: Array<String>) {
      if (args.isEmpty())
        return err.println("Usage: <classes-dir> ...custom.Type=tsType")
      val dir = Path.of(args[0])
      val customTypes = args.drop(1).associate { it.split("=").let { it[0] to it[1] } }
      TSGenerator(customTypes).generate(dir)
    }
  }
}

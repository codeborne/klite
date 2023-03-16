package klite.json

import klite.Converter
import klite.publicProperties
import org.intellij.lang.annotations.Language
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

/** Converts project data classes/enums to TypeScript for front-end type-safety */
class TSGenerator(
  private val customTypes: Map<String, String> = emptyMap(),
  private val typePrefix: String = "export "
) {
  @OptIn(ExperimentalPathApi::class)
  fun scan(dir: Path) {
    dir.walk(INCLUDE_DIRECTORIES).filter { it.extension == "class" }.forEach {
      try {
        val cls = Class.forName(dir.relativize(it).toString().removeSuffix(".class").replace("/", ".")).kotlin
        render(cls)?.let { println(typePrefix + it) }
      } catch (ignore: UnsupportedOperationException) {
      } catch (e: ClassNotFoundException) {
        err.println("// $e")
      }
    }
  }

  @Language("TypeScript") fun render(cls: KClass<*>) =
    if (cls.isData || cls.java.isInterface && !cls.java.isAnnotation) renderInterface(cls)
    else if (cls.isValue) renderInline(cls)
    else if (cls.isSubclassOf(Enum::class)) renderEnum(cls)
    else null

  private fun renderEnum(cls: KClass<*>) = "enum " + tsName(cls) + " {" + cls.java.enumConstants.joinToString { "$it = '$it'" } + "}"

  private fun renderInline(cls: KClass<*>) = "type " + tsName(cls) +
    (cls.typeParameters.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") ?: "") +
    " = " + tsType(cls.memberProperties.first().returnType)

  private fun renderInterface(cls: KClass<*>): String {
    val sb = StringBuilder()
    sb.append("interface ").append(tsName(cls)).append(" {")
    (cls.publicProperties as Sequence<KProperty1<Any, *>>).notIgnored.forEach { p ->
      sb.append(p.jsonName)
      if (p.returnType.isMarkedNullable) sb.append("?")
      sb.append(": ").append(tsType(p.returnType)).append("; ")
    }
    if (sb.endsWith("; ")) sb.setLength(sb.length - 2)
    return sb.append("}").toString()
  }

  private fun tsType(type: KType): String {
    val cls = type.classifier as KClass<*>
    return customTypes[type.toString()] ?: (when {
      cls.isValue -> tsName(cls)
      cls.isSubclassOf(Enum::class) -> tsName(cls)
      cls.isSubclassOf(Boolean::class) -> "boolean"
      cls.isSubclassOf(Number::class) -> "number"
      cls.isSubclassOf(Iterable::class) || cls.java.isArray -> "Array"
      cls.isSubclassOf(Map::class) -> "Record"
      cls.isSubclassOf(CharSequence::class) || Converter.supports(cls) -> "string"
      cls.isData -> tsName(cls)
      else -> "any"
    } + (type.arguments.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") { tsType(it.type!!) } ?: ""))
  }

  private fun tsName(type: KClass<*>) = type.java.name.substringAfterLast(".").replace("$", "")
}

fun main(args: Array<String>) {
  if (args.isEmpty())
    return err.println("Usage: <classes-dir> <custom-types>")
  val dir = Path.of(args[0])
  val customTypes = args.drop(1).associate { it.split("=").let { it[0] to it[1] } }
  TSGenerator(customTypes).scan(dir)
}

package klite.json

import klite.Converter
import klite.Email
import klite.Phone
import klite.publicProperties
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.PrintStream
import java.lang.System.err
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.time.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption.INCLUDE_DIRECTORIES
import kotlin.io.path.extension
import kotlin.io.path.walk
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/** Converts project data/enum/inline classes to TypeScript for front-end type-safety */
open class TSGenerator(
  customTypes: Map<String, String?> = emptyMap(),
  private val typePrefix: String = "export ",
  private val out: PrintStream = System.out
) {
  private val customTypes = defaultCustomTypes + customTypes

  @OptIn(ExperimentalPathApi::class)
  open fun printFrom(dir: Path) {
    dir.walk(INCLUDE_DIRECTORIES).filter { it.extension == "class" }.sorted().forEach {
      val className = dir.relativize(it).toString().removeSuffix(".class").replace(File.separatorChar, '.')
      printClass(className)
    }
  }

  protected open fun printUnmappedCustomTypes() = customTypes.forEach {
    if (it.value == null) printClass(it.key)
  }

  protected open fun printClass(className: String) = try {
    val cls = Class.forName(className).kotlin
    render(cls)?.let {
      out.println("// $cls")
      out.println(typePrefix + it)
    }
  } catch (ignore: UnsupportedOperationException) {
  } catch (e: Exception) {
    err.println("// $className: $e")
  }

  @Language("TypeScript") open fun render(cls: KClass<*>) =
    if (cls.isData || cls.java.isInterface && !cls.java.isAnnotation) renderInterface(cls)
    else if (cls.isValue) renderInline(cls)
    else if (cls.isSubclassOf(Enum::class)) renderEnum(cls)
    else null

  protected open fun renderEnum(cls: KClass<*>) = "enum " + tsName(cls) + " {" + cls.java.enumConstants.joinToString { "$it = '$it'" } + "}"

  protected open fun renderInline(cls: KClass<*>) = "type " + tsName(cls) + typeParams(cls, noVariance = true) +
    " = " + tsType(cls.primaryConstructor?.parameters?.first()?.type)

  protected open fun typeParams(cls: KClass<*>, noVariance: Boolean = false) =
    cls.typeParameters.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") { if (noVariance) it.name else it.toString() } ?: ""

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
    val ts = customTypes[type?.toString()] ?: customTypes[type?.jvmErasure?.qualifiedName] ?: when {
      cls == null || cls == Any::class -> "any"
      cls.isValue -> tsName(cls)
      cls.isSubclassOf(Enum::class) -> tsName(cls)
      cls.isSubclassOf(Boolean::class) -> "boolean"
      cls.isSubclassOf(Number::class) -> "number"
      cls.isSubclassOf(Iterable::class) -> "Array"
      cls.java.isArray -> "Array" + (cls.java.componentType?.let { if (it.isPrimitive) "<" + tsType(it.kotlin.createType()) + ">" else "" } ?: "")
      cls.isSubclassOf(Map::class) -> "Record"
      cls == KProperty1::class -> "keyof " + tsType(type.arguments.first().type)
      cls.isSubclassOf(CharSequence::class) || Converter.supports(cls) -> "string"
      cls.isData || cls.java.isInterface -> tsName(cls)
      else -> "any"
    }
    return if (ts[0].isLowerCase()) ts
    else ts + (type?.arguments?.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") { tsType(it.type) } ?: "")
  }

  protected open fun tsName(type: KClass<*>) = type.java.name.substringAfterLast(".").replace("$", "")

  companion object {
    const val tsDate = "\${number}-\${number}-\${number}"
    const val tsTime = "\${number}:\${number}:\${number}"
    const val tsUrl = "`\${string}://\${string}`"

    val defaultCustomTypes = mapOf(
      LocalDate::class to "`${tsDate}`",
      LocalTime::class to "`${tsTime}`",
      LocalDateTime::class to "`${tsDate}T${tsTime}`",
      OffsetDateTime::class to "`${tsDate}T${tsTime}+\${number}:\${number}`",
      Instant::class to "`${tsDate}T${tsTime}Z`",
      URL::class to tsUrl,
      URI::class to tsUrl,
      Email::class to "`\${string}@\${string}`",
      Phone::class to "`+\${number}`",
    ).mapKeys { it.key.qualifiedName!! }

    @JvmStatic fun main(args: Array<String>) {
      if (args.isEmpty())
        return err.println("Usage: <classes-dir> ...custom.Type=tsType ...package.IncludeThisType [-o <output-file>] [-p <prepend-text>]")

      val dir = Path.of(args[0])
      val argsLeft = args.toMutableList().apply { removeAt(0) }
      val out = argsLeft.arg("-o")?.let { PrintStream(it, UTF_8) } ?: System.out
      out.use {
        argsLeft.arg("-p")?.let { out.println(it) }
        val customTypes = argsLeft.associate { it.split("=").let { it[0] to it.getOrNull(1) } }
        TSGenerator(customTypes, out = out).apply {
          printUnmappedCustomTypes()
          printFrom(dir)
        }
      }
    }

    @JvmStatic private fun MutableList<String>.arg(prefix: String): String? {
      val i = indexOf(prefix)
      return if (i == -1) null else get(i + 1).also { removeAt(i); removeAt(i) }
    }
  }
}

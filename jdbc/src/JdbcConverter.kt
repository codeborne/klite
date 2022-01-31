package klite.jdbc

import klite.Converter
import klite.annotations.annotation
import java.lang.reflect.Modifier.isStatic
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.sql.Connection
import java.sql.Date
import java.sql.Timestamp
import java.time.*
import java.time.ZoneOffset.UTC
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

typealias ToJdbcConverter<T> = (T, Connection?) -> Any

object JdbcConverter {
  val nativeTypes: MutableSet<KClass<*>> = mutableSetOf(
    UUID::class, BigDecimal::class, BigInteger::class, LocalDate::class, LocalDateTime::class, LocalTime::class, OffsetDateTime::class
  )
  private val converters: MutableMap<KClass<*>, ToJdbcConverter<*>> = ConcurrentHashMap()

  init {
    use<Instant> { v, _ -> v.atOffset(UTC) }

    val toString: ToJdbcConverter<Any> = { v, _ -> v.toString() }
    use<Period>(toString)
    use<Duration>(toString)
    use<Currency>(toString)
    use<Locale>(toString)
    use<URL>(toString)
    use<URI>(toString)
  }

  operator fun <T: Any> set(type: KClass<T>, converter: ToJdbcConverter<T>) { converters[type] = converter }
  inline fun <reified T: Any> use(noinline converter: ToJdbcConverter<T>) = set(T::class, converter)

  fun to(v: Any?, conn: Connection? = null) = when (v) {
    null -> null
    is Enum<*> -> v.name
    is Collection<*> -> conn!!.createArrayOf(if (v.firstOrNull() is UUID) "uuid" else "varchar", v.toTypedArray())
    else -> {
      val cls = v::class
      @Suppress("UNCHECKED_CAST") when {
        cls.javaPrimitiveType != null || nativeTypes.contains(cls) -> v
        converters.contains(cls) -> (converters[cls] as ToJdbcConverter<Any>).invoke(v, conn)
        cls.annotation<JvmInline>() != null -> unwrapInline(v)
        Converter.supports(v::class) -> v.toString()
        else -> v
      }
    }
  }

  private fun unwrapInline(v: Any) =
    v.javaClass.declaredFields.first { !isStatic(it.modifiers) }.apply { isAccessible = true }.get(v)

  fun from(v: Any?, target: KType): Any? = when(target) {
    Instant::class -> (v as Timestamp).toInstant()
    LocalDate::class -> (v as? Date)?.toLocalDate()
    LocalDateTime::class -> (v as Timestamp).toLocalDateTime()
    else -> when {
      v is String && target.jvmErasure != String::class -> Converter.from(v, target)
      v is java.sql.Array && target.jvmErasure == Set::class -> (v.array as Array<*>).map { from(it, target.arguments[0].type!!) }.toSet()
      v is java.sql.Array && target.jvmErasure.isSubclassOf(Iterable::class) -> (v.array as Array<*>).map { from(it, target.arguments[0].type!!) }.toList()
      else -> v
    }
  }
}

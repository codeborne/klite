package klite.jdbc

import klite.Converter
import klite.Decimal
import klite.annotations.annotation
import klite.d
import klite.unboxInline
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
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
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
    is Collection<*> -> conn!!.createArrayOf(arrayType(v.firstOrNull()?.javaClass), v.toTypedArray())
    is Array<*> -> conn!!.createArrayOf(arrayType(v.javaClass.componentType), v)
    else -> {
      val cls = v::class
      @Suppress("UNCHECKED_CAST") when {
        cls.javaPrimitiveType != null || nativeTypes.contains(cls) -> v
        converters.contains(cls) -> (converters[cls] as ToJdbcConverter<Any>).invoke(v, conn)
        cls.isValue && cls.hasAnnotation<JvmInline>() -> v.unboxInline()
        Converter.supports(v::class) -> v.toString()
        else -> v
      }
    }
  }

  private fun arrayType(c: Class<*>?) = when {
    c == null -> "varchar"
    UUID::class.java.isAssignableFrom(c) -> "uuid"
    Number::class.java.isAssignableFrom(c) -> "numeric"
    LocalDate::class.java.isAssignableFrom(c) -> "date"
    LocalTime::class.java.isAssignableFrom(c) -> "time"
    LocalDateTime::class.java.isAssignableFrom(c) -> "timestamp"
    Instant::class.java.isAssignableFrom(c) -> "timestamptz"
    else -> "varchar"
  }

  fun from(v: Any?, target: KType): Any? = if (v is java.sql.Array) {
    val targetClass = target.jvmErasure
    if (targetClass.java.isArray) v.array else {
      val list = (v.array as Array<*>).map { from(it, target.arguments[0].type!!) }
      if (targetClass == Set::class) list.toSet()
      else list
    }
  } else from(v, target.jvmErasure)

  fun from(v: Any?, target: KClass<*>?): Any? = when(target) {
    Instant::class -> (v as? Timestamp)?.toInstant()
    LocalDate::class -> (v as? Date)?.toLocalDate()
    LocalDateTime::class -> (v as? Timestamp)?.toLocalDateTime()
    Decimal::class -> v?.toString()?.d
    else -> if (target?.annotation<JvmInline>() != null || target == Decimal::class) target.primaryConstructor!!.call(v)
    else if (v is String && target != null && target != String::class) Converter.from(v, target) else v
  }
}

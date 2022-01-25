package klite.jdbc

import klite.Converter
import klite.annotations.annotation
import java.lang.reflect.Modifier.isStatic
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.sql.Connection
import java.time.*
import java.time.ZoneOffset.UTC
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

typealias ToJdbcConverter<T> = (T, Connection) -> Any

object JdbcConverter {
  val nativeTypes: MutableSet<KClass<*>> = mutableSetOf(
    UUID::class, BigDecimal::class, BigInteger::class, LocalDate::class, LocalDateTime::class
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

  fun toDBType(v: Any?, conn: Connection) = when (v) {
    null -> null
    is Enum<*> -> v.name
    is Collection<*> -> conn.createArrayOf(if (v.firstOrNull() is UUID) "uuid" else "varchar", v.toTypedArray())
    else -> {
      val cls = v::class
      @Suppress("UNCHECKED_CAST") when {
        nativeTypes.contains(cls) -> v
        converters.contains(cls) -> (converters[cls] as ToJdbcConverter<Any>).invoke(v, conn)
        cls.annotation<JvmInline>() != null -> unwrapInline(v)
        Converter.supports(v::class) -> v.toString()
        else -> v
      }
    }
  }

  private fun unwrapInline(v: Any) =
    v.javaClass.declaredFields.first { !isStatic(it.modifiers) }.apply { isAccessible = true }.get(v)
}

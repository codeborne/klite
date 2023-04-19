package klite

import klite.Decimal.Companion.CENTS
import java.lang.Math.*
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * A monetary amount with 2 fraction digits, lighter than BigDecimal and having numeric equals.
 * Operations resulting in Long overflow will throw ArithmeticException.
 */
class Decimal internal constructor(private val c: Long): Comparable<Decimal>, Number() {
  companion object {
    const val CENTS = 100L
    @JvmStatic val MAX_VALUE = Decimal(Long.MAX_VALUE)
    @JvmStatic val MIN_VALUE = Decimal(Long.MIN_VALUE)
    @JvmStatic val ZERO = Decimal(0)
    @JvmStatic val ONE = Decimal(CENTS)
  }

  constructor(v: Double): this((v * CENTS).roundToLong())
  constructor(v: String): this(v.toDouble())

  operator fun unaryMinus() = Decimal(-c)
  operator fun unaryPlus() = this

  operator fun plus(o: Decimal) = Decimal(addExact(c, o.c))
  operator fun minus(o: Decimal) = Decimal(subtractExact(c, o.c))
  operator fun times(o: Decimal) = Decimal(roundDiv(multiplyExact(c, o.c), CENTS))
  operator fun div(o: Decimal) = Decimal(roundDiv(c, o.toLong()))
  operator fun rem(o: Decimal) = Decimal(c % o.c)

  operator fun times(o: Int) = Decimal(multiplyExact(c, o))
  operator fun times(o: Long) = Decimal(multiplyExact(c, o))
  operator fun times(o: Double) = Decimal(toDouble() * o)
  operator fun div(o: Int) = div(o.toLong())
  operator fun div(o: Long) = Decimal(roundDiv(c, o))
  operator fun div(o: Double) = Decimal(toDouble() / o)

  private fun roundDiv(n: Long, d: Long) = (if ((n < 0) xor (d < 0)) subtractExact(n, d/2) else addExact(n, d/2)) / d

  operator fun inc() = plus(1.d)
  operator fun dec() = minus(1.d)

  fun abs() = if (c < 0) Decimal(-c) else this
  infix fun percent(p: Decimal) = times(p) / CENTS

  override fun equals(o: Any?) = c == (o as? Decimal)?.c
  override fun hashCode() = c.hashCode()
  override fun compareTo(o: Decimal) = c.compareTo(o.c)

  override fun toString() = "${c / CENTS}.${(c % CENTS).absoluteValue.toString().padStart(2, '0')}"

  override fun toDouble() = c / CENTS.toDouble()
  override fun toFloat() = toDouble().toFloat()
  override fun toLong() = roundDiv(c, CENTS)
  override fun toInt() = toLong().toInt()
  override fun toShort() = toLong().toShort()
  override fun toByte() = toLong().toByte()
  override fun toChar() = toInt().toChar()
}

val Int.d get() = Decimal(multiplyExact(CENTS, this))
val Long.d get() = Decimal(multiplyExact(CENTS, this))
val Double.d get() = Decimal(this)
val Float.d get() = Decimal(toDouble())
val String.d get() = Decimal(this)

fun Iterable<Decimal>.sum() = sumOf { it }
fun Collection<Decimal>.average() = sum() / size.d

inline fun <T> Sequence<T>.sumOf(selector: (T) -> Decimal): Decimal = asIterable().sumOf(selector)
inline fun <T> Iterable<T>.sumOf(selector: (T) -> Decimal): Decimal {
  var sum = Decimal.ZERO
  for (element in this) sum += selector(element)
  return sum
}

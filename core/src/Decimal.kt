package klite

import klite.Decimal.Companion.DECIMALS
import java.lang.Math.*
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/** A monetary amount with 2 fraction digits, lighter than BigDecimal and having numeric equals */
class Decimal internal constructor(private val c: Long): Comparable<Decimal>, Number() {
  companion object {
    const val DECIMALS = 100L
    @JvmStatic val MAX_VALUE = Decimal(Long.MAX_VALUE)
    @JvmStatic val MIN_VALUE = Decimal(Long.MIN_VALUE)
    @JvmStatic val ZERO = Decimal(0)
    @JvmStatic val ONE = Decimal(DECIMALS)
  }

  constructor(v: Double): this((v * DECIMALS).roundToLong())
  constructor(v: String): this(v.split('.').let { it[0].toLong() * DECIMALS + (it.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0) })

  operator fun unaryMinus() = Decimal(-c)
  operator fun unaryPlus() = this

  operator fun plus(o: Decimal) = Decimal(addExact(c, o.c))
  operator fun minus(o: Decimal) = Decimal(subtractExact(c, o.c))
  operator fun times(o: Decimal) = Decimal(multiplyExact(c, o.c) / DECIMALS)
  operator fun div(o: Decimal) = Decimal(toDouble() / o.toDouble())
  operator fun rem(o: Decimal) = Decimal(c % o.c)

  operator fun inc() = plus(1.d)
  operator fun dec() = minus(1.d)

  operator fun times(o: Long) = Decimal(multiplyExact(c, o))
  operator fun div(o: Long) = Decimal(c / o)

  infix fun percent(p: Decimal) = times(p) / DECIMALS

  override fun equals(o: Any?) = c == (o as? Decimal)?.c
  override fun hashCode() = c.hashCode()
  override fun compareTo(o: Decimal) = c.compareTo(o.c)

  override fun toString() = "${c / DECIMALS}.${(c % DECIMALS).absoluteValue.toString().padStart(2, '0')}"

  override fun toDouble() = c / DECIMALS.toDouble()
  override fun toFloat() = c / DECIMALS.toFloat()
  override fun toLong() = c / DECIMALS
  override fun toInt() = toLong().toInt()
  override fun toShort() = toLong().toShort()
  override fun toByte() = toLong().toByte()
  override fun toChar() = toInt().toChar()
}

val Int.d get() = Decimal(multiplyExact(DECIMALS, this))
val Long.d get() = Decimal(multiplyExact(DECIMALS, this))
val Double.d get() = Decimal(this)
val Float.d get() = Decimal(toDouble())
val String.d get() = Decimal(this)

package klite

import kotlin.math.absoluteValue
import kotlin.math.roundToLong

internal class Monetary private constructor(private val c: Long): Comparable<Monetary>, Number() {
  companion object {
    const val DECIMALS = 100L
    val MAX_VALUE = Monetary(Long.MAX_VALUE)
    val MIN_VALUE = Monetary(Long.MIN_VALUE)
  }

  constructor(v: Int): this(v * DECIMALS)
  // TODO: Long
  constructor(v: Double): this((v * DECIMALS).roundToLong())
  constructor(v: Float): this((v * DECIMALS).roundToLong())
  constructor(v: String): this(v.split('.').let { it[0].toLong() * DECIMALS + (it.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0) })

  operator fun unaryMinus() = Monetary(-c)
  operator fun unaryPlus() = this

  operator fun plus(o: Monetary) = Monetary(Math.addExact(c, o.c))
  operator fun minus(o: Monetary) = Monetary(Math.subtractExact(c, o.c))
  operator fun times(o: Monetary) = Monetary(Math.multiplyExact(c, o.c) / DECIMALS)
  operator fun div(o: Monetary) = Monetary(toDouble() / o.toDouble())

  operator fun inc() = plus(1.m)
  operator fun dec() = minus(1.m)

  operator fun times(o: Long) = Monetary(Math.multiplyExact(c, o))
  operator fun div(o: Long) = Monetary(c / o)

  override fun equals(o: Any?) = c == (o as? Monetary)?.c
  override fun hashCode() = c.hashCode()
  override fun compareTo(o: Monetary) = c.compareTo(o.c)

  override fun toString() = "${c / DECIMALS}.${(c % DECIMALS).absoluteValue.toString().padStart(2, '0')}"

  override fun toDouble() = c / DECIMALS.toDouble()
  override fun toFloat() = c / DECIMALS.toFloat()
  override fun toLong() = c / DECIMALS
  override fun toInt() = toLong().toInt()
  override fun toShort() = toLong().toShort()
  override fun toByte() = toLong().toByte()
  override fun toChar() = toInt().toChar()
}

internal val Int.m get() = Monetary(this)
internal val String.m get() = Monetary(this)

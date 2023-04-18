package klite

import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal class Monetary private constructor(private val v: Long): Comparable<Monetary>, Number() {
  companion object {
    const val DECIMALS = 100L
    val MAX_VALUE = Monetary(Long.MAX_VALUE / DECIMALS)
    val MIN_VALUE = Monetary(Long.MIN_VALUE / DECIMALS)
  }

  constructor(v: Int): this(v * DECIMALS)
  // TODO: Long
  constructor(v: Double): this((v * DECIMALS).roundToLong())
  constructor(v: Float): this((v * DECIMALS).roundToLong())
  constructor(v: String): this(v.split('.').let { it[0].toLong() * DECIMALS + (it.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0) })

  operator fun unaryMinus() = Monetary(-v)
  operator fun unaryPlus() = this

  operator fun plus(o: Monetary) = Monetary(Math.addExact(v, o.v))
  operator fun minus(o: Monetary) = Monetary(Math.subtractExact(v, o.v))
  operator fun times(o: Monetary) = Monetary(Math.multiplyExact(v, o.v) / DECIMALS)
  operator fun div(o: Monetary) = Monetary(toDouble() / o.toDouble())

  operator fun times(o: Long) = Monetary(Math.multiplyExact(v, o))
  operator fun div(o: Long) = Monetary(v / o)

  override fun equals(o: Any?) = v == (o as? Monetary)?.v
  override fun hashCode() = v.hashCode()
  override fun compareTo(o: Monetary) = v.compareTo(o.v)

  override fun toString() = "${v / DECIMALS}.${(v % DECIMALS).toString().padStart(2, '0')}"

  override fun toDouble() = v / DECIMALS.toDouble()
  override fun toFloat() = v / DECIMALS.toFloat()
  override fun toLong() = v / DECIMALS
  override fun toInt() = toLong().toInt()
  override fun toShort() = toLong().toShort()
  override fun toByte() = toLong().toByte()
  override fun toChar() = toInt().toChar()
}

internal val Int.m get() = Monetary(this)
internal val String.m get() = Monetary(this)

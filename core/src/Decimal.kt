package klite

import java.math.BigDecimal

/** A monetary value, with numeric equality */
class Decimal: BigDecimal {
  constructor(value: String): super(value)
  constructor(value: Int): super(value)
  constructor(value: Long): super(value)
  constructor(value: Double): super(value)

  override fun equals(other: Any?) = other is BigDecimal && compareTo(other) == 0
  override fun hashCode() = if (scale() == 0) super.hashCode() else stripTrailingZeros().hashCode()
  override fun toString(): String = toPlainString()

  // kotlin.Number requires these...
  override fun toByte() = byteValueExact()
  override fun toChar() = intValueExact().toChar()
  override fun toShort() = shortValueExact()
}

val Int.d get() = Decimal(this)
val Long.d get() = Decimal(this)
val String.d get() = Decimal(this)
val Double.d get() = Decimal(this)

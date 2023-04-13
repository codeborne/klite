package klite

internal class Monetary private constructor(private val v: Long): Comparable<Monetary> {
  companion object {
    const val DECIMALS = 100L
    val MAX_VALUE = Monetary(Long.MAX_VALUE / DECIMALS)
    val MIN_VALUE = Monetary(Long.MIN_VALUE / DECIMALS)
  }

  constructor(v: Int): this(v * DECIMALS)
  // TODO: Long
  constructor(v: Double): this((v * DECIMALS).toLong())
  constructor(v: Float): this((v * DECIMALS).toLong())
  constructor(v: String): this(v.split('.').let { it[0].toLong() * DECIMALS + (it.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0) })

  operator fun plus(o: Monetary) = Monetary(v + o.v)
  operator fun minus(o: Monetary) = Monetary(v - o.v)
  operator fun times(o: Monetary) = Monetary(v * o.v / DECIMALS)
  operator fun div(o: Monetary) = Monetary(v * DECIMALS / o.v)

  override fun equals(o: Any?) = v == (o as? Monetary)?.v
  override fun hashCode() = v.hashCode()
  override fun compareTo(o: Monetary) = v.compareTo(o.v)

  override fun toString() = "${v / DECIMALS}.${(v % DECIMALS).toString().padStart(2, '0')}"
}

internal val Int.m get() = Monetary(this)
internal val String.m get() = Monetary(this)

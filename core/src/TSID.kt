package klite

import java.lang.System.currentTimeMillis
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/** Time-Sorted unique ID, a more compact and index-friendly alternative to UUID */
@JvmInline value class TSID(val value: Long) {
  companion object {
    const val RANDOM_BITS = 22
    const val RANDOM_MASK = 0x003fffff
    val EPOCH = Instant.parse("2022-10-21T03:45:00.000Z").toEpochMilli()
    val random = SecureRandom()
    val counter = AtomicInteger()
    @Volatile var lastTime = 0L

    private fun generate(): Long {
      val time = (currentTimeMillis() - EPOCH) shl RANDOM_BITS
      if (time != lastTime) {
        counter.set(random.nextInt())
        lastTime = time
      }
      val tail = counter.incrementAndGet() and RANDOM_MASK
      return time or tail.toLong()
    }

    init {
      Converter.use { TSID(it) }
    }
  }

  constructor(): this(generate())
  constructor(tsid: String): this(tsid.toLong(36))
  override fun toString() = value.toString(36)
}

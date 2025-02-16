package klite

import java.lang.System.currentTimeMillis
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Time-Sorted unique ID, a more compact and DB index-friendly alternative to UUID.
 * Add a `typealias Id<T> = TSID<T>` or `Id = TSID<Any>` in your own project.
 */
@JvmInline value class TSID<T>(val value: Long) {
  companion object {
    const val RANDOM_BITS = 22
    const val RANDOM_MASK = 0x003fffff
    val EPOCH = Instant.parse(Config.optional("TSID_EPOCH", "2022-10-21T03:45:00.000Z")).toEpochMilli()
    private var random = SecureRandom()
    private val counter = AtomicInteger()
    @Volatile private var lastTime = 0L
    var deterministic: AtomicLong? = null

    fun generateValue(): Long {
      deterministic?.let { return it.incrementAndGet() }
      val time = (currentTimeMillis() - EPOCH) shl RANDOM_BITS
      if (time != lastTime) {
        counter.set(random.nextInt())
        lastTime = time
      }
      val tail = counter.incrementAndGet() and RANDOM_MASK
      return time or tail.toLong()
    }

    init {
      Converter.use { TSID<Any>(it) }
    }
  }

  constructor(): this(generateValue())
  constructor(tsid: String): this(tsid.toLong(36))
  override fun toString() = value.toString(36)

  val createdAt: Instant get() = Instant.ofEpochMilli((value shr RANDOM_BITS) + EPOCH)
}

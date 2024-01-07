package klite

import java.util.concurrent.atomic.AtomicLong

open class RequestIdGenerator {
  val prefix = (0xFFFF * Math.random()).toInt().toString(16)
  private val counter = AtomicLong()
  open operator fun invoke(headers: Headers) = "$prefix-${counter.incrementAndGet()}"
}

open class XRequestIdGenerator: RequestIdGenerator() {
  override fun invoke(headers: Headers) = super.invoke(headers) + (headers.getFirst("X-Request-Id")?.let { "/$it" } ?: "")
}

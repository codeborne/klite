package klite.slf4j

/**
 * Use this logger if you want shorter stack traces:
 * `Config["LOGGER_CLASS"] = StackTraceOptimizingLogger::class.qualifiedName!!`
 */
open class StackTraceOptimizingLogger(name: String): KliteLogger(name) {
  public override fun print(formatted: String, t: Throwable?): Unit = synchronized(out) {
    if (formatted.isNotEmpty()) {
      out.print(formatted)
      if (t == null) out.println()
      else if (!formatted.endsWith(" ")) out.print(": ")
    }
    if (t != null) {
      val stackTrace = t.stackTrace
      out.println(t.toString())
      for (i in 0..findUsefulStackTraceEnd(stackTrace)) {
        out.print("  at ")
        out.println(stackTrace[i].toString())
      }
      t.cause?.let { print("Caused by", it) }
    }
  }

  protected fun findUsefulStackTraceEnd(trace: Array<out StackTraceElement>): Int {
    var until = trace.lastIndex
    val predicate: StackTraceElement.() -> Boolean = { className.run { startsWith("klite") || contains(".coroutines.") } }
    while (until > 0 && !trace[until].predicate()) until--
    while (until > 0 && trace[until].predicate()) until--
    return if (until < trace.lastIndex) until + 1 else until
  }
}

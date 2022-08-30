package klite.slf4j

/**
 * Use this logger if you want stack traces to be logged as a single log line in json format:
 * Config["LOGGER_CLASS"] = StacktraceOptimizingJsonLogger::class
 */
open class StackTraceOptimizingJsonLogger(name: String): StackTraceOptimizingLogger(name) {
  override fun print(formatted: String, t: Throwable?) {
    if (formatted.isNotEmpty()) {
      out.print(formatted)
      out.print(" ")
    }
    if (t != null) printJson(t)
    out.println()
  }

  private fun printJson(t: Throwable) {
    out.print("""{"error": """")
    out.print(t.toString().replace("\"", "\\\"").replace("\n", "\\n"))
    out.print("""", "stack": {""")
    val stackTrace = t.stackTrace
    val until = findUsefulStackTraceEnd(stackTrace)
    for (i in 0..until) {
      out.print("\""); out.print(i); out.print("\": \"")
      out.print(stackTrace[i].toString())
      out.print("\"")
      if (i != until) out.print(", ")
    }
    t.cause?.let {
      out.print(""", "cause": """)
      printJson(it)
    }
    out.print("}}")
  }
}

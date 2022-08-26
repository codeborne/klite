package klite.slf4j

/**
 * Use this logger if you want stack traces to be logged as a single log line in json format:
 * Config["LOGGER_CLASS"] = StacktraceOptimizingJsonLogger::class
 */
open class StackTraceOptimizingJsonLogger(name: String): StackTraceOptimizingLogger(name) {
  override fun print(formatted: String, t: Throwable?) {
    out.print(formatted)
    if (t == null) out.println()
    else {
      if (formatted.isNotEmpty() && !formatted.endsWith(" ")) out.print(" ")
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
      out.println("}}")
    }
  }
}

package klite.slf4j

/**
 * Use this logger if you want stack traces to be logged as a single log line in json format:
 * Config["LOGGER_CLASS"] = StacktraceOptimizingJsonLogger::class
 */
open class StackTraceOptimizingJsonLogger(name: String): StackTraceOptimizingLogger(name) {
  override fun print(formatted: String, t: Throwable?) {
    val sb = StringBuilder()
    if (formatted.isNotEmpty()) {
      sb.append(formatted)
      sb.append(" ")
    }
    if (t != null) appendJson(sb, t)
    out.println(sb.toString())
  }

  private fun appendJson(sb: StringBuilder, t: Throwable) {
    sb.append("""{"error": """")
    sb.append(t.toString().replace("\"", "\\\"").replace("\n", "\\n"))
    sb.append("""", "stack": {""")
    val stackTrace = t.stackTrace
    val until = findUsefulStackTraceEnd(stackTrace)
    for (i in 0..until) {
      sb.append("\""); sb.append(i); sb.append("\": \"")
      sb.append(stackTrace[i].toString())
      sb.append("\"")
      if (i != until) sb.append(", ")
    }
    t.cause?.let {
      sb.append(""", "cause": """)
      appendJson(sb, it)
    }
    sb.append("}}")
  }
}

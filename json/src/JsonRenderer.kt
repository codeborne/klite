package klite.json

import java.io.Writer
import java.util.*

class JsonRenderer(private val out: Writer, private val opts: JsonOptions): AutoCloseable {
  fun render(o: Any?) = writeValue(o)

  private fun writeValue(o: Any?) {
    when (o) {
      is String -> { write('\"'); write(opts.valueConverter(o.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"")).toString()); write('\"') }
      is Iterable<*> -> {
        write('[')
        o.firstOrNull()?.let { writeValue(it) }
        o.drop(1).forEach { write(','); writeValue(it) }
        write(']')
      }
      is Array<*> -> writeValue(Arrays.asList(*o))
      is Map<*, *> -> {
        write('{')
        o.entries.apply {
          firstOrNull()?.let(::writeEntry)
          drop(1).forEach { write(','); writeEntry(it) }
        }
        write('}')
      }
      else -> write(opts.valueConverter(o).toString())
    }
  }

  private fun writeEntry(it: Map.Entry<Any?, Any?>) {
    writeValue(opts.keyConverter(it.key.toString()))
    write(':')
    writeValue(it.value)
  }

  private fun write(c: Char) = out.write(c.code)
  private fun write(s: String) = out.write(s)

  override fun close() = out.close()
}

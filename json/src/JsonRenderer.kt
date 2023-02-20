package klite.json

import klite.Converter
import klite.toValues
import java.io.Writer
import java.util.*

class JsonRenderer(private val out: Writer, private val opts: JsonOptions): AutoCloseable {
  fun render(o: Any?) = writeValue(o)

  private fun writeValue(o: Any?) {
    when (o) {
      is String -> { write('\"'); write(opts.values.toJson(o.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"")).toString()); write('\"') }
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
          drop(1).forEach {
            // TODO: JsonIgnore, JsonProperty
            write(','); writeEntry(it)
          }
        }
        write('}')
      }
      null, is Number, is Boolean -> write(o.toString())
      else -> if (Converter.supports(o::class)) writeValue(opts.values.toJson(o).let { if (it !== o) it else it.toString() })
              else writeValue(opts.values.toJson(o).let { if (it !== o) it else it.toValues() })
    }
  }

  private fun writeEntry(it: Map.Entry<Any?, Any?>) {
    writeValue(opts.keys.toJson(it.key.toString()))
    write(':')
    writeValue(it.value)
  }

  private fun write(c: Char) = out.write(c.code)
  private fun write(s: String) = out.write(s)

  override fun close() = out.close()
}

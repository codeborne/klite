package klite.json

import klite.Converter
import klite.toValues
import java.io.Writer
import java.util.*
import kotlin.reflect.full.hasAnnotation

class JsonRenderer(private val out: Writer, private val opts: JsonOptions): AutoCloseable {
  fun render(o: Any?) = writeValue(o)

  private fun writeValue(o: Any?) {
    when (o) {
      is String -> { write('\"'); write(opts.values.to(o.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"")).toString()); write('\"') }
      is Iterable<*> -> {
        write('[')
        val i = o.iterator()
        if (i.hasNext()) writeValue(i.next())
        i.forEachRemaining { write(','); writeValue(it) }
        write(']')
      }
      is Array<*> -> writeValue(Arrays.asList(*o))
      is Map<*, *> -> {
        write('{')
        val i = o.iterator()
        if (i.hasNext()) writeEntry(i.next())
        // TODO: JsonIgnore, JsonProperty
        i.forEachRemaining { write(','); writeEntry(it) }
        write('}')
      }
      null, is Number, is Boolean -> write(o.toString())
      else ->
        if (o::class.hasAnnotation<JvmInline>()) writeValue(o.javaClass.getMethod("unbox-impl").invoke(o))
        else if (Converter.supports(o::class)) writeValue(opts.values.to(o).let { if (it !== o) it else it.toString() })
        else writeValue(opts.values.to(o).let { if (it !== o) it else it.toValues() })
    }
  }

  private fun writeEntry(it: Map.Entry<Any?, Any?>) {
    writeValue(opts.keys.to(it.key.toString()))
    write(':')
    writeValue(it.value)
  }

  private fun write(c: Char) = out.write(c.code)
  private fun write(s: String) = out.write(s)

  override fun close() = out.close()
}

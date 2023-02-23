package klite.json

import klite.*
import java.io.Writer
import java.util.*
import java.util.Map.entry
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class JsonRenderer(private val out: Writer, private val opts: JsonOptions): AutoCloseable {
  fun render(o: Any?) = writeValue(o)

  private fun writeValue(o: Any?) {
    when (o) {
      is String -> { write('\"'); write(opts.values.to(o.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"")).toString()); write('\"') }
      is Iterable<*> -> writeArray(o)
      is Array<*> -> writeArray(Arrays.asList(*o))
      is Map<*, *> -> writeObject(o.iterator())
      null, is Number, is Boolean -> write(o.toString())
      else ->
        if (o::class.hasAnnotation<JvmInline>()) writeValue(o.unboxInline())
        else if (Converter.supports(o::class)) writeValue(opts.values.to(o).let { if (it !== o) it else it.toString() })
        else opts.values.to(o).let { if (it !== o) writeValue(it) else writeObject(it) }
    }
  }

  private fun writeArray(o: Iterable<*>) {
    write('[')
    val i = o.iterator()
    if (i.hasNext()) writeValue(i.next())
    i.forEachRemaining { write(','); writeValue(it) }
    write(']')
  }

  private fun writeObject(i: Iterator<Map.Entry<Any?, Any?>>) {
    write('{')
    if (i.hasNext()) writeEntry(i.next())
    i.forEachRemaining { write(','); writeEntry(it) }
    write('}')
  }

  private fun writeObject(o: Any) = writeObject(o.propsSequence.filter { it.visibility == PUBLIC && !it.hasAnnotation<JsonIgnore>() }
    .map { entry(it.findAnnotation<JsonProperty>()?.value?.trimToNull() ?: it.name, it.valueOf(o)) }.iterator())

  private fun writeEntry(it: Map.Entry<Any?, Any?>) {
    writeValue(opts.keys.to(it.key.toString()))
    write(':')
    writeValue(it.value)
  }

  private fun write(c: Char) = out.write(c.code)
  private fun write(s: String) = out.write(s)

  override fun close() = out.close()
}

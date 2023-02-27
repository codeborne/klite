package klite.json

import klite.*
import java.io.Writer
import java.util.*
import java.util.AbstractMap.SimpleImmutableEntry
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class JsonRenderer(private val out: Writer, private val opts: JsonMapper): AutoCloseable {
  fun render(o: Any?) = writeValue(o)

  private fun writeValue(o: Any?) {
    when (o) {
      is String -> { write('\"'); write(opts.values.to(o.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b").replace("\"", "\\\"")).toString()); write('\"') }
      is Iterable<*> -> writeArray(o)
      is Array<*> -> writeArray(Arrays.asList(*o))
      is Map<*, *> -> writeObject(o.asSequence())
      null, is Number, is Boolean -> write(o.toString())
      else ->
        if (o::class.isValue && o::class.hasAnnotation<JvmInline>()) writeValue(o.unboxInline())
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

  private fun writeObject(o: Sequence<Map.Entry<Any?, Any?>>) {
    val i = (if (opts.renderNulls) o else o.filter { it.value != null }).iterator()
    write('{')
    if (i.hasNext()) writeEntry(i.next())
    i.forEachRemaining { write(','); writeEntry(it) }
    write('}')
  }

  private fun writeObject(o: Any) = writeObject(o.publicProperties.filter { !it.hasAnnotation<JsonIgnore>() }
    .map { SimpleImmutableEntry(it.findAnnotation<JsonProperty>()?.value?.trimToNull() ?: it.name, it.valueOf(o)) })

  private fun writeEntry(it: Map.Entry<Any?, Any?>) {
    writeValue(opts.keys.to(it.key.toString()))
    write(':')
    writeValue(it.value)
  }

  private fun write(c: Char) = out.write(c.code)
  private fun write(s: String) = out.write(s)

  override fun close() = out.close()
}

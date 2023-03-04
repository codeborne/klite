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
      is CharSequence -> writeString(opts.values.to(o).toString())
      is Iterable<*> -> writeArray(o)
      is Array<*> -> writeArray(Arrays.asList(*o))
      is Map<*, *> -> writeObject(o.asSequence())
      null, is Number, is Boolean -> write(o.toString())
      else ->
        if (Converter.supports(o::class)) writeValue(opts.values.to(o).let { if (it !== o) it else it.toString() })
        else if (o::class.isValue && o::class.hasAnnotation<JvmInline>()) writeValue(o.unboxInline())
        else opts.values.to(o).let { if (it !== o) writeValue(it) else writeObject(it) }
    }
  }

  private fun writeString(s: String) {
    write('\"')
    s.forEach { when(it) {
      '\n' -> write("\\n"); '\r' -> write("\\r"); '\t' -> write("\\t"); '"' -> write("\\\"")
      in '\u0000'..'\u001F' -> { write("\\u"); write(it.code.toString(16).padStart(4, '0')) }
      else -> write(it)
    } }
    write('\"')
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

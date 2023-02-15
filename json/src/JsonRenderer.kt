package klite.json

import java.io.Writer
import java.util.*

class JsonRenderer(private val out: Writer, private val opts: JsonOptions): AutoCloseable {
  fun render(o: Any?) = renderValue(o)

  private fun renderValue(o: Any?) {
    when (o) {
      is String -> { write('\"'); write(o); write('\"') }
      is Iterable<*> -> {
        write('[')
        o.firstOrNull()?.let { renderValue(it) }
        o.drop(1).forEach { write(','); renderValue(it) }
        write(']')
      }
      is Array<*> -> renderValue(Arrays.asList(*o))
      else -> write(o.toString())
    }
  }

  private fun write(c: Char) = out.write(c.code)
  private fun write(s: String) = out.write(s)

  override fun close() = out.close()
}

package klite.json

import java.io.Writer

class JsonRenderer(private val out: Writer, private val opts: JsonOptions): AutoCloseable {
  fun render(o: Any?) = renderValue(o)

  private fun renderValue(o: Any?) = when(o) {
    is String -> out.apply { write('\"'.code); write(o); write('\"'.code); }
    else -> out.write(o.toString())
  }

  override fun close() = out.close()
}

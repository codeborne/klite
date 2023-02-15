package klite.json

import java.io.Writer

class JsonRenderer(private val opts: JsonOptions) {
  fun render(o: Any?, out: Writer) = out.write(o.toString())
}

package klite

import java.io.OutputStream

data class ServerSentEvent(val data: Any? = "", val event: String? = null, val id: Any? = null) {
  internal fun sendTo(out: OutputStream, dataRenderer: BodyRenderer? = null) {
    id?.let { out.write("id: $it\n") }
    event?.let { out.write("event: $it\n") }
    out.write("data: ")
    dataRenderer?.render(out, data) ?: out.write(data.toString().replace("\n", "\ndata: "))
    out.write("\n\n")
    out.flush()
  }
}

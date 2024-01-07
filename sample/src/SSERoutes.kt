import klite.HttpExchange
import klite.ServerSentEvent
import klite.annotations.GET
import klite.annotations.POST
import klite.annotations.Path
import klite.info
import klite.logger
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

@Path("/sse")
class SSERoutes {
  private val queue = ArrayBlockingQueue<Message>(100)
  private val log = logger("sse")

  data class Message(val hello: String)

  // curl --data '{"hello":"World"}' -H 'Content-Type: application/json' http://localhost:8080/api/sse
  @POST fun post(message: Message) = queue.put(message)

  // use EventSource in browser to receive
  @GET fun listen(e: HttpExchange) = try {
    e.startEventStream()
    while (true) e.send(ServerSentEvent(queue.take()))
  } catch (e: IOException) {
    log.info(e.toString()) // client disconnect
  }

  @GET("/demo") suspend fun coroutineDemo(e: HttpExchange) {
    e.startEventStream()
    e.send(ServerSentEvent(event = "start"))
    try {
      for (i in 1..100) {
        val data = mapOf("message" to "Hello $i")
        e.send(ServerSentEvent(data = data, id = UUID.randomUUID()))
        log.info("Sent $data")
        delay(2000)
      }
      e.send(ServerSentEvent(event = "end"))
    } catch (e: IOException) {
      log.info(e.toString()) // client disconnect
    }
  }
}

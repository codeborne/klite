package klite

import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThan
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import kotlin.random.Random

class ServerTest {
  @Test fun `bind server to any available port`() {
    val server = Server(listen = InetSocketAddress( 0))
    try {
      server.start(gracefulStopDelaySec = -1)
      expect(server.listen.port).toBeGreaterThan(0)
    } finally {
      server.stop(0)
    }
  }

  @Test fun `bind server to custom port`() {
    val port = Random.nextInt(1024, 65535) + 1
    val server = Server(listen = InetSocketAddress(port))
    try {
      server.start(gracefulStopDelaySec = -1)
      expect(server.listen.port).toEqual(port)
    } finally {
      server.stop(0)
    }
  }

  @Test fun `expose bound address if server is not started`() {
    expect(Server().listen.port).toEqual(8080)
  }
}


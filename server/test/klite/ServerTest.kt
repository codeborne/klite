package klite

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import kotlin.random.Random

class ServerTest {
  @Test fun `bind server to any available port`() {
    val server = Server(listen = InetSocketAddress(0))
    try {
      server.start(gracefulStopDelaySec = -1)
      expect(server.address.port).toBeGreaterThan(0)
    } finally {
      server.stop(0)
    }
  }

  @Test fun `bind server to custom port`() {
    val port = Random.nextInt(1024, 65535) + 1
    val server = Server(listen = InetSocketAddress(port))
    try {
      server.start(gracefulStopDelaySec = -1)
      expect(server.address.port).toEqual(port)
    } finally {
      server.stop(0)
    }
  }

  val server = Server()

  @Test fun `expose bound address if server is started`() {
    expect(server.listen.port).toEqual(8080)
    expect { server.address.port }.toThrow<IllegalStateException>().message.toEqual("Server not started")
  }

  @Test fun `use Extension`() {
    var installed = false
    val extension = object: Extension {
      override fun install(server: Server) {
        installed = true
      }
    }
    server.use(extension)
    expect(installed).toEqual(true)
  }

  @Test fun `use Runnable`() {
    val extension = mockk<Runnable>(relaxUnitFun = true)
    server.use(extension)
    verify { extension.run() }
  }

  @Test fun `use BodyRenderer & BodyParser`() {
    val body = TextBody()
    server.use(body)
    expect(server.renderers).toContain(body)
    expect(server.parsers).toContain(body)
  }
}


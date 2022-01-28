package klite

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beNull
import org.junit.jupiter.api.Test

annotation class Public

class RouteTest {
  @Test fun annotations() {
    val route = Route(RequestMethod.GET, "".toRegex(), {})
    expect(route.annotation<Public>()).to.beNull()

    val annotated = Route(RequestMethod.GET, "".toRegex(), {}, listOf(Public()))
    expect(annotated.annotation<Public>()).to.not.beNull()
  }
}

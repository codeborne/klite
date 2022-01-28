package klite

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull

annotation class Public

class RouteTest {
  @Test fun annotations() {
    val route = Route(RequestMethod.GET, "".toRegex(), {})
    expectThat(route.annotation<Public>()).isNull()

    val annotated = Route(RequestMethod.GET, "".toRegex(), {}, listOf(Public()))
    expectThat(annotated.annotation<Public>()).isNotNull()
  }
}

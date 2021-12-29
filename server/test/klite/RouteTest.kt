package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

annotation class Public

class RouteTest {
  @Test fun annotations() {
    val route = Route(RequestMethod.GET, "".toRegex(), {})
    assertThat(route.annotation<Public>()).isNull()

    val annotated = Route(RequestMethod.GET, "".toRegex(), {}, listOf(Public()))
    assertThat(annotated.annotation<Public>()).isNotNull
  }
}

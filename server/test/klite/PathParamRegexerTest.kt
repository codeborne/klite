package klite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PathParamRegexerTest {
  val paramRegexer = PathParamRegexer()

  @Test fun plainPath() {
    val plainPath = paramRegexer.from("/hello")
    assertThat(plainPath.matches("/hello")).isTrue
    assertThat(plainPath.matches("/hello2")).isFalse
    assertThat(plainPath.matches("/hello/2")).isFalse
    assertThat(plainPath.matches("/world")).isFalse
  }

  @Test fun paramPath() {
    val plainPath = paramRegexer.from("/x/:param1")
    assertThat(plainPath.matches("/x/123")).isTrue
    assertThat(plainPath.matchEntire("/x/123")!!.groups["param1"]!!.value).isEqualTo("123")
    assertThat(plainPath.matchEntire("/xy/123")).isNull()
    assertThat(plainPath.matchEntire("/x/123/222")).isNull()
  }

  @Test fun multipleParams() {
    val plainPath = paramRegexer.from("/:p1/:p2/")
    val match = plainPath.matchEntire("/123/456/")!!
    assertThat(match.groups["p1"]!!.value).isEqualTo("123")
    assertThat(match.groups["p2"]!!.value).isEqualTo("456")
  }
}

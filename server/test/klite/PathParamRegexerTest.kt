package klite

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

class PathParamRegexerTest {
  val paramRegexer = PathParamRegexer()

  @Test fun plainPath() {
    val plainPath = paramRegexer.from("/hello")
    expectThat(plainPath.matches("/hello")).isTrue()
    expectThat(plainPath.matches("/hello2")).isFalse()
    expectThat(plainPath.matches("/hello/2")).isFalse()
    expectThat(plainPath.matches("/world")).isFalse()
  }

  @Test fun paramPath() {
    val plainPath = paramRegexer.from("/x/:param1")
    expectThat(plainPath.matches("/x/123")).isTrue()
    expectThat(plainPath.matchEntire("/x/123")!!.groups["param1"]!!.value).isEqualTo("123")
    expectThat(plainPath.matchEntire("/xy/123")).isNull()
    expectThat(plainPath.matchEntire("/x/123/222")).isNull()
  }

  @Test fun multipleParams() {
    val plainPath = paramRegexer.from("/:p1/:p2/")
    val match = plainPath.matchEntire("/123/456/")!!
    expectThat(match.groups["p1"]!!.value).isEqualTo("123")
    expectThat(match.groups["p2"]!!.value).isEqualTo("456")
  }
}

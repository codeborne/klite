package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class PathParamRegexerTest {
  val paramRegexer = PathParamRegexer()

  @Test fun plainPath() {
    val plainPath = paramRegexer.from("/hello")
    expect(plainPath.matches("/hello")).toEqual(true)
    expect(plainPath.matches("/hello2")).toEqual(false)
    expect(plainPath.matches("/hello/2")).toEqual(false)
    expect(plainPath.matches("/world")).toEqual(false)
  }

  @Test fun paramPath() {
    val plainPath = paramRegexer.from("/x/:param1")
    expect(plainPath.matches("/x/123")).toEqual(true)
    expect(plainPath.matchEntire("/x/123")!!.groups["param1"]!!.value).toEqual("123")
    expect(plainPath.matchEntire("/xy/123")).toEqual(null)
    expect(plainPath.matchEntire("/x/123/222")).toEqual(null)
  }

  @Test fun multipleParams() {
    val plainPath = paramRegexer.from("/:p1/:p2/")
    val match = plainPath.matchEntire("/123/456/")!!
    expect(match.groups["p1"]!!.value).toEqual("123")
    expect(match.groups["p2"]!!.value).toEqual("456")
  }
}

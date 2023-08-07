package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class PathParamRegexerTest {
  val paramRegexer = PathParamRegexer()

  @Test fun plainPath() {
    val pathRegex = paramRegexer.from("/hello")
    expect(pathRegex.matches("/hello")).toEqual(true)
    expect(pathRegex.matches("/hello2")).toEqual(false)
    expect(pathRegex.matches("/hello/2")).toEqual(false)
    expect(pathRegex.matches("/world")).toEqual(false)
  }

  @Test fun paramPath() {
    val pathRegex = paramRegexer.from("/x/:param1")
    expect(pathRegex.matches("/x/123")).toEqual(true)
    expect(pathRegex.matchEntire("/x/123")!!.groups["param1"]!!.value).toEqual("123")
    expect(pathRegex.matchEntire("/xy/123")).toEqual(null)
    expect(pathRegex.matchEntire("/x/123/222")).toEqual(null)
  }

  @Test fun multipleParams() {
    val pathRegex = paramRegexer.from("/:p1/:p2/")
    val match = pathRegex.matchEntire("/123/456/")!!
    expect(match.groups["p1"]!!.value).toEqual("123")
    expect(match.groups["p2"]!!.value).toEqual("456")
  }

  @Test fun toOpenApi() {
    expect(paramRegexer.toOpenApi("/:p1/:p2")).toEqual("/{p1}/{p2}")
    expect(paramRegexer.toOpenApi(paramRegexer.from("/:p1/:p2"))).toEqual("/{p1}/{p2}")
  }

  @Test fun noSlashPrefix() {
    val pathRegex = paramRegexer.from(":p1/:p2")
    val match = pathRegex.matchEntire("123/456")!!
    expect(match.groups["p1"]!!.value).toEqual("123")
    expect(match.groups["p2"]!!.value).toEqual("456")
  }
}

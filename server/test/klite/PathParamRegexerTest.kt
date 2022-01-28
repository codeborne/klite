package klite

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.beFalse
import net.oddpoet.expect.extension.beNull
import net.oddpoet.expect.extension.beTrue
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test

class PathParamRegexerTest {
  val paramRegexer = PathParamRegexer()

  @Test fun plainPath() {
    val plainPath = paramRegexer.from("/hello")
    expect(plainPath.matches("/hello")).to.beTrue()
    expect(plainPath.matches("/hello2")).to.beFalse()
    expect(plainPath.matches("/hello/2")).to.beFalse()
    expect(plainPath.matches("/world")).to.beFalse()
  }

  @Test fun paramPath() {
    val plainPath = paramRegexer.from("/x/:param1")
    expect(plainPath.matches("/x/123")).to.beTrue()
    expect(plainPath.matchEntire("/x/123")!!.groups["param1"]!!.value).to.equal("123")
    expect(plainPath.matchEntire("/xy/123")).to.beNull()
    expect(plainPath.matchEntire("/x/123/222")).to.beNull()
  }

  @Test fun multipleParams() {
    val plainPath = paramRegexer.from("/:p1/:p2/")
    val match = plainPath.matchEntire("/123/456/")!!
    expect(match.groups["p1"]!!.value).to.equal("123")
    expect(match.groups["p2"]!!.value).to.equal("456")
  }
}

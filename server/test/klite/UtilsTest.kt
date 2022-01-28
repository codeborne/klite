package klite

import net.oddpoet.expect.expect
import net.oddpoet.expect.extension.equal
import org.junit.jupiter.api.Test

class UtilsTest {
  @Test fun urlParams() {
    val params = mapOf("Hello" to "Wörld", "1" to "2", "null" to null)
    expect(urlEncodeParams(params)).to.equal("Hello=W%C3%B6rld&1=2")
    expect(urlDecodeParams("Hello=W%C3%B6rld&1=2")).to.equal(params - "null")
  }
}

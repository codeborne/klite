package klite.xml

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class XMLParserTest {
  val parser = XMLParser()

  @Language("XML")
  val xml = """
      <transportMovement>
        <id schemeAgencyId="AGENCY1">123</id>
        <modeCode>SEA</modeCode>
        <dangerousGoodsIndicator>true</dangerousGoodsIndicator>
      </transportMovement>
    """.trimIndent().byteInputStream()

  @Language("XML")
  val xmlWithNamespaces = """
      <x:transportMovement xmlns:x="http://example.com/x" xmlns:z="http://example.com/z">
        <z:id z:schemeAgencyId="AGENCY1">123</z:id>
        <z:modeCode>SEA</z:modeCode>
        <x:dangerousGoodsIndicator>true</x:dangerousGoodsIndicator>
      </x:transportMovement>
    """.trimIndent().byteInputStream()

  @Test fun parse() {
    expect(parser.parse<Identifier>(xml)).toEqual(
      Identifier("123", "AGENCY1", "SEA", dangerousGoods = true))
  }

  @Test fun namespaces() {
    expect(parser.parse<Identifier>(xmlWithNamespaces)).toEqual(
      Identifier("123", "AGENCY1", "SEA", dangerousGoods = true))
  }

  @Test fun parsePathMap() {
    expect(parser.parsePathMap(xmlWithNamespaces)).toEqual(
      mapOf(
        "/transportMovement/id" to "123",
        "/transportMovement/id/@schemeAgencyId" to "AGENCY1",
        "/transportMovement/modeCode" to "SEA",
        "/transportMovement/dangerousGoodsIndicator" to "true",
      ))
  }

  data class Identifier(
    @XmlPath("/transportMovement/id") // from root
    val id: String,

    @XmlPath("id/@schemeAgencyId") // relative
    val type: String,

    @XmlPath("modeCode")
    val mode: String,

    @XmlPath("dangerousGoodsIndicator")
    val dangerousGoods: Boolean = false
  )
}

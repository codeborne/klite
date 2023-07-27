package klite.csv

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.d
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class CSVGeneratorTest {
  val out = ByteArrayOutputStream()

  @Test fun `generate with comma`() {
    CSVGenerator(out).apply {
      row("column1", "column2")
      row("Hello", "World")
      row(1.25, 2)
    }

    expect(out.toString()).toEqual("\uFEFFcolumn1,column2\nHello,World\n1.25,2\n")
  }

  @Test fun `generate with semicolon for Estonian`() {
    CSVGenerator(out, separator = ";").apply {
      row("Hello", "World", "OÜ \"Mets ja koer\";xxx")
      row(1.25, 2, 3.75.d)
    }
    expect(out.toString()).toEqual("""
      ﻿Hello;World;"OÜ ""Mets ja koer"";xxx"
      1,25;2;3,75

    """.trimIndent())
  }
}

package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class ChangeSetTest {
  val changeSet = ChangeSet("id")

  @Test fun empty() {
    expect(changeSet.statements).toBeEmpty()
  }

  @Test fun `multiple statements`() {
    val sql = """
      create table blah(
        id number
      );
      create index on blah(id);
    """.trimIndent()
    sql.split("\n").forEach(changeSet::addLine)

    expect(changeSet.sql.toString()).toEqual(sql)
    expect(changeSet.statements).toContainExactly(sql.substringBefore(";"), sql.substringAfter(";").substringBefore(";"))
    expect(changeSet.checksum).toEqual(-105362483148)
  }

  @Test fun `no trailing separator`() {
    changeSet.addLine("hello")
    changeSet.finish()
    expect(changeSet.statements).toContainExactly("hello")
  }

  @Test fun `no separator`() {
    val changeSet = changeSet.copy(separator = null)
    changeSet.addLine("hello")
    changeSet.finish()
    expect(changeSet.statements).toContainExactly("hello")
  }

  @Test fun matches() {
    expect(changeSet.matches(setOf("anything"))).toEqual(true)
    expect(changeSet.copy(context = "something").matches(setOf("anything"))).toEqual(false)
    expect(changeSet.copy(context = "!prod").matches(setOf("anything"))).toEqual(true)
    expect(changeSet.copy(context = "!prod").matches(setOf("x", "prod"))).toEqual(false)
  }
}

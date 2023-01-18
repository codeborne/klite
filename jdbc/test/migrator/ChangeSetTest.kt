package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toBeEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class ChangeSetTest {
  @Test fun empty() {
    expect(ChangeSet("id").statements).toBeEmpty()
  }

  @Test fun `pre-created`() {
    val changeSet = ChangeSet("hello", "hello;world;\n  xxx;  ")
    expect(changeSet.statements).toContainExactly("hello", "world", "xxx")
    expect(changeSet.checksum).toEqual(795550245100)
  }

  @Test fun `multiple statements by lines`() {
    val changeSet = ChangeSet("id")
    val sql = """
      create table blah(
        id number
      );
      create index on blah(id);
    """.trimIndent()
    sql.split("\n").forEach(changeSet::addLine)
    changeSet.finish()

    expect(changeSet.sql.toString()).toEqual(sql)
    expect(changeSet.statements).toContainExactly(sql.substringBefore(";"), sql.substringAfter(";").substringBefore(";").trim())
    expect(changeSet.checksum).toEqual(-106065226198)
  }

  @Test fun checksum() {
    expect(ChangeSet("").checksum).toEqual(null)
    expect(ChangeSet("", "xxx").checksum).toEqual("xxx".hashCode().toLong())
    expect(ChangeSet("", checksum = 567).checksum).toEqual(567)
  }

  @Test fun `no separator`() {
    val changeSet = ChangeSet("", "hello;world", separator = null)
    expect(changeSet.statements).toContainExactly("hello;world")
  }

  @Test fun matches() {
    val changeSet = ChangeSet("")
    expect(changeSet.matches(setOf("anything"))).toEqual(true)
    expect(changeSet.copy(context = "something").matches(setOf("anything"))).toEqual(false)
    expect(changeSet.copy(context = "!prod").matches(setOf("anything"))).toEqual(true)
    expect(changeSet.copy(context = "!prod").matches(setOf("x", "prod"))).toEqual(false)
  }
}

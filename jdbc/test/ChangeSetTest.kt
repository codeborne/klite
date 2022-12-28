package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class ChangeSetTest {
  @Test fun `multiple statements`() {
    val changeSet = ChangeSet("id")
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
}

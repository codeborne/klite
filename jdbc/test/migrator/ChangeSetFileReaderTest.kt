package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toEndWith
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toHaveSize
import ch.tutteli.atrium.api.fluent.en_GB.toStartWith
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class ChangeSetFileReaderTest {
  val changeSets = ChangeSetFileReader("migrator/init.sql")

  @Test fun read() {
    val list = changeSets.toList()
    expect(list).toHaveSize(1)
    val dbChangelog = list.first()
    expect(dbChangelog.id).toEqual("db_changelog")
    expect(dbChangelog.statements).toHaveSize(1)
    expect(dbChangelog.statements.first()).toStartWith("create table").toEndWith(")")
    expect(dbChangelog.checksum).toEqual(-1707762516)
  }
}

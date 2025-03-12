package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import klite.Config
import klite.jdbc.ChangeSet.On.RUN
import klite.jdbc.ChangeSet.On.SKIP
import org.junit.jupiter.api.Test

class ChangeSetFileReaderTest {
  @Test fun init() {
    val list = ChangeSetFileReader("migrator/init.sql").toList()
    expect(list).toHaveSize(1)
    val dbChangelog = list.first()
    expect(dbChangelog.id).toEqual("db_changelog")
    expect(dbChangelog.statements).toHaveSize(1)
    expect(dbChangelog.statements.first()).toStartWith("create table").toEndWith(")")
    expect(dbChangelog.checksum).toEqual(-1707762516)
  }

  @Test fun `args and substitutions`() {
    Config["APP_PASS"] = "some pass"
    val list = ChangeSetFileReader("migrator/test.sql").toList()
    expect(list).toHaveSize(3)

    val test = list.first()
    expect(test.copy(sql = test.sql.toString())).toEqual(
      ChangeSet("test", "begin; exec hello($\${json}$$); password='some pass'; text='''--'; end;", filePath = "migrator/test.sql", onChange = RUN, onFail = SKIP, separator = "xxx", context = "!prod", checksum = 5052932))

    val test2 = list[1]
    expect(test2.copy(sql = test2.sql.toString())).toEqual(
      ChangeSet("multiline", """
        create table something(
          id text primary key,

          createdAt date
        );
      """.trimIndent(), filePath = "migrator/test.sql", checksum = 1038158909))

    val test3 = list.last()
    expect(test3.copy(sql = test3.sql.toString())).toEqual(
      ChangeSet("test2", "checksum overridden;", filePath = "migrator/test.sql", checksum = 123))
  }

  @Test fun invalid() {
    expect { ChangeSetFileReader("migrator/invalid.sql").toList() }
      .toThrow<IllegalArgumentException>().messageToContain("Unknown changeset param invalid")
  }
}

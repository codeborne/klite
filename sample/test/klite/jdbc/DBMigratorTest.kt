package klite.sample.klite.jdbc

import klite.jdbc.*
import klite.sample.DBTest
import org.junit.jupiter.api.Test

class DBMigratorTest: DBTest() {
  @Test fun dropAllOnFailure() {
    val changeSet = ChangeSet("changing", "create sequence hello", checksum = 123)
    ChangeSetRepository(db).save(changeSet)
    Transaction.current()!!.close()
    DBMigrator(db, sequenceOf(changeSet.copy(checksum = 345)) + ChangeSetFileReader("db.sql"), dropAllOnFailure = true).migrate()
  }
}

package klite.sample

import klite.Config
import klite.jdbc.DBMigrator

abstract class DBTest: klite.jdbc.DBTest() {
  companion object {
    init {
      Config["ENV"] = "test,test-data"
      Config["DB_URL"] += "_test"
      DBMigrator(db, dropAllOnFailure = true).migrate()
    }
  }
}

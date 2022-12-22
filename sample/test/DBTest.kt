package klite.sample

import klite.Config
import klite.jdbc.DBMigrator
import klite.jdbc.SimpleDataSource

abstract class DBTest: klite.jdbc.DBTest() {
  companion object {
    init {
      Config["ENV"] = "test,test-data"
      Config["DB_URL"] += "_test"
      DBMigrator(SimpleDataSource(), listOf("db.sql"), mapOf(
        "id" to "id uuid default gen_random_uuid() primary key",
        "createdAt" to "createdAt timestamptz default current_timestamp"
      )).migrate()
    }
  }
}

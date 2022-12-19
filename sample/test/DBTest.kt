package klite.sample

import klite.Config
import klite.jdbc.DBMigrator
import klite.jdbc.SimpleDataSource

abstract class DBTest: klite.jdbc.DBTest() {
  companion object {
    init {
      Config["ENV"] = "test,test-data"
      Config["DB_URL"] += "_test"
      DBMigrator(SimpleDataSource(), listOf("db.sql", "users.sql"), mapOf(
        "id" to "id uuid default uuid_generate_v4() primary key",
        "createdAt" to "createdAt timestamptz default current_timestamp"
      )).migrate()
    }
  }
}

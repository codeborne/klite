package klite.sample

import klite.Config
import klite.liquibase.LiquibaseModule

abstract class DBTest: klite.jdbc.DBTest() {
  companion object {
    init {
      Config["DB_URL"] += "_test"
      LiquibaseModule().migrate(listOf("test", "test-data"))
    }
  }
}

package klite.jdbc.users

import klite.liquibase.LiquibaseModule

abstract class DBTest: klite.jdbc.DBTest() {
  companion object {
    init {
      LiquibaseModule().migrate(listOf("test", "test-data"))
    }
  }
}

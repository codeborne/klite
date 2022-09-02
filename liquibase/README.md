# klite-liquibase

Provides [LiquibaseModule](src/LiquibaseModule.kt) to migrate the DB on application start.

As Liquibase uses java.logging module, this module will also redirect it to slf4j to be used together with `klite-jdbc`.

## Use a different use for migration and application

It is a best practice in terms of security to use a user with fewer right for the application.

Your Liquibase scripts can actually create this user without the "create table" and "drop table" permissions.

```kotlin
  // migrate with the default all-rights user
  use(LiquibaseModule())
  // use app user for the application DataSource and connection pool
  useAppDBUser()
  use(DBModule {
    // override any other connection pool settings
  })
```

# klite-liquibase

Provides [LiquibaseModule](src/LiquibaseModule.kt) to migrate the DB on application start.

As Liquibase uses java.logging module, this will also redirect it to slf4j to be used together with `klite-jdbc`.

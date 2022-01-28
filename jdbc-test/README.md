# klite-jdbc-test

Provides `DBTest` base class for tests that run using the real DB, e.g. in Docker.
Depend on this project from your tests (`testImplementation` in Gradle).

You project decides how the DB itself is started:

You can either tell the developers to run e.g. `docker-compose up -d db` manually,
but something like TestContainers can also be used, but will probably make tests start more slowly.

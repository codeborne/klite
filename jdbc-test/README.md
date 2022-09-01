# klite-jdbc-test

Provides `DBTest` base class for tests that run using the real DB, e.g. in Docker.
Depend on this project from your tests (`testImplementation` in Gradle).

By default, `docker-compose up -d db` will be tried to start test db automatically.
Use `DB_START` env variable to either disable or configure the docker-compose service name.

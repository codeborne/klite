# klite sample

This is a small sample application of how to use klite, which also uses the database.

Start with [Launcher](src/Launcher.kt)

## Tests

Running of klite tests also require starting a DB using (which is attempted to start automatically):

```docker compose up -d db```

This is generally much faster than using e.g. TestContainers, as you start DB once on your development machine
and use it for both the app and tests, which is really fast.

Some [klite-jdbc](../jdbc) tests are thus in this sample subproject.

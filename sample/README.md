# klite sample

This is a small sample application how to use klite.

Start with [Launcher](src/Launcher.kt)

## Tests

Running of klite tests also require starting a DB using:

```docker-compose up -d db```

This is generally much faster than using e.g. TestContainers, as you start DB once on your development machine
and use it for both the app and tests, which is really fast.

Some [klite-jdbc](../jdbc) tests are thus in this sample subproject.

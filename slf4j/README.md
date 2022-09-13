# klite-slf4j

As many Java libraries already depend on slf4j-api instead of the built-in System.Logger,
adding this dependency will redirect System.Logger to slf4j and use a simple implementation for
actual lightweight logging to System.out.

The following Config properties are supported:
* `LOGGER=INFO` - to set the default logger level
* `LOGGER.logger.name=DEBUG` - to set some logger to another level (with usual level inheritance from parent logger)

If you want to redefine the logging format, define extend [KliteLogger](src/KliteLogger.kt) and specify it's full class name as `LOGGER_CLASS`.

When deploying as a 12-factor app, this is all you need - logging to standard out.

## Other slf4j backends

This module uses the slf4j 2.0 ServiceLoader mechanism to provide a simple logger to slf4j.

If you want to switch to another (bigger) backend (e.g. logback), then ensure that it supports slfj4 2.0 and that it is before this module on classpath.

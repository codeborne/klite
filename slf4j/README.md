# klite-slf4j

As many Java libraries already depend on slf4j-api instead of the built-in System.Logger,
adding this dependency will redirect System.Logger to slf4j and provide a simple implementation for
logging to System.out.

The following Config properties are supported:
* `LOGGER=INFO` - to set the default logger level
* `LOGGER.logger.name=DEBUG` - to set some logger to another level (with usual level inheritance from parent logger)

If you want to redefine the logging format, extend [KliteLogger](src/KliteLogger.kt) and specify it's full class name as `LOGGER_CLASS`.

There are two non-default implementations available:
* `klite.slf4j.StackTraceOptimizingLogger` - this one will omit some lower stack trace frames that are not very useful
* `klite.slf4j.StackTraceOptimizingJsonLogger` - this one will output exception and stack trace as a single-line json, friendly for log indexing services

When deploying as a 12-factor app, this is all you need - logging to standard out.

## Other slf4j backends

This module uses the slf4j 2.x ServiceLoader mechanism to provide a simple logger to slf4j.
This implementation is much lighter than `slf4j-simple.jar` and is easier to extend/configure.

If you want to use another (much bigger) backend (e.g. logback), then ensure that it supports slf4j 2.x ServiceLoader and that it is before this module on classpath.

# klite-slf4j

As many Java libraries already depend on slf4j-api instead of the built-in System.Logger,
including this dependency will redirect System.Logger to slf4j and use slf4j-simple.jar for
actual lightweight logging to System.out.

Read SimpleLogger documentation if you want to customize logging format.

When deploying as 12-factor app, this is all you need - logging to standard out.

# kotlin-server

At attempt of very light-weight non-blocking http app template with support for Kotlin coroutines.

Inspired by SparkJava, Jooby, etc, but simpler and better.

## Goals

* Proper Kotlin coroutine support with working before/after filters for e.g. transactions and logging
* Minimal amount of code
  * Simple to maintain & change
  * Performance is also important, but simplicity is preferred
* Zero dependencies - Java built-in jdk.httpserver is used under the hood
  * Perfect for microservices 
  * But still possible to replace with something else
* Most behaviour can be overridden if needed
* Both route builder and annotated classes
* Very easy to do simple things, e.g.
  `@GET fun route() = provider.fetchData()` 

## TODO 
* authorization checks with Forbidden/Unauthorized by default
* api key support?
* session as signed cookie (works with multiple nodes by default)

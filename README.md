# Klite

[![Build & Test](https://github.com/angryziber/kotlin-server/actions/workflows/ci.yml/badge.svg)](https://github.com/angryziber/kotlin-server/actions/workflows/ci.yml)

A very light-weight (lite) non-blocking http framework for Kotlin coroutines on JVM.

Inspired by SparkJava, Jooby, etc, but smaller, simpler and better.

## Goals

* Proper Kotlin coroutine support with working before/after filters for e.g. transactions and logging
* Minimal amount of code
  * Simple to maintain & change
  * Performance is also important, but simplicity is preferred
* Zero dependencies - Java built-in **jdk.httpserver** is used under the hood
  * Perfect for microservices
  * But still possible to easily add support for other servers if needed
  * [Sample docker image](sample/Dockerfile) is about 50-70Mb thanks to jlink, depending on number of used components
* 12-factor apps by default
* Most behaviour can be overridden if necessary
* Both route builder and annotated classes
* Very easy to do simple things, e.g.
  `@GET suspend fun route() = provider.fetchData()`
* Not much need for documentation - the source code is short and readable.

## Dependencies

* Java 6+ built-in non-blocking jdk.httpserver
* Re-routable Java 9+ System.Logger

# Modules

* [server](server) - the main server module. See [it's docs](server). Zero external dependencies.
* [jackson](jackson) - adds json parsing/rendering using Jackson
* [serialization](serialization) - adds json parsing/rendering using kotlinx-serialization
* [slf4j](slf4j) - redirects server logs to slf4j and configures it
* [jdbc](jdbc) - provides jdbc extensions and transaction handling (depends on slf4j because of Hikari)
* [jdbc-test](jdbc-test) - provides a way of testing your DB code using a real DB
* [liquibase](liquibase) - allows to use liquibase for DB migration

## Usage

See [sample subproject](sample) on how to build apps with Klite and run them in Docker.

[![Release](https://jitpack.io/v/angryziber/klite.svg)](https://jitpack.io/#angryziber/klite)

Klite builds are available from [jitpack](https://jitpack.io):
```kt
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  dependencies {
    val kliteVersion = "master-SNAPSHOT" // you can put a released tag or commit hash here
    implementation("com.github.angryziber.klite:klite-server:$kliteVersion")
    // Plus any optional components with their own external dependencies, see above for list
    implementation("com.github.angryziber.klite:klite-jackson:$kliteVersion")
    implementation("com.github.angryziber.klite:klite-jdbc:$kliteVersion")
    testImplementation("com.github.angryziber.klite:klite-jdbc-test:$kliteVersion")
    ...
  }
```

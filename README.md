# Klite

[![Build & Test](https://github.com/angryziber/kotlin-server/actions/workflows/ci.yml/badge.svg)](https://github.com/angryziber/kotlin-server/actions/workflows/ci.yml)

A very light-weight (lite) non-blocking http framework for Kotlin coroutines.

Inspired by SparkJava, Jooby, etc, but smaller, simpler and better.

## Goals

* Proper Kotlin coroutine support with working before/after filters for e.g. transactions and logging
* Minimal amount of code
  * Simple to maintain & change
  * Performance is also important, but simplicity is preferred
* Zero dependencies - Java built-in jdk.httpserver is used under the hood
  * Perfect for microservices
  * But still possible to replace with something else
* 12-factor apps by default
* Most behaviour can be overridden if necessary
* Both route builder and annotated classes
* Very easy to do simple things, e.g.
  `@GET suspend fun route() = provider.fetchData()`

## TODO
* authorization checks with Forbidden/Unauthorized by default
* api key support?
* session as signed cookie (works with multiple nodes by default)

## Dependencies

* Java 6+ built-in non-blocking jdk.httpserver
* Re-routable Java 9+ System.Logger

## Usage

[![Release](https://jitpack.io/v/angryziber/klite.svg)](https://jitpack.io/#angryziber/klite)

Currently, you can request klite to be built and served by [jitpack](https://jitpack.org):

```kt
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  dependencies {
    val kliteVersion = "master-SNAPSHOT" // you can put a released tag or commit hash here
    implementation("com.github.angryziber.klite:server:$kliteVersion")
    implementation("com.github.angryziber.klite:jackson:$kliteVersion")
    implementation("com.github.angryziber.klite:jdbc:$kliteVersion")
    testImplementation("com.github.angryziber.klite:jdbc-test:$kliteVersion")
    ...
  }
```

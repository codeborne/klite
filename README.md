# Klite

[![Build & Test](https://github.com/codeborne/klite/actions/workflows/ci.yml/badge.svg)](https://github.com/codeborne/klite/actions/workflows/ci.yml)

A very light-weight (lite) non-blocking http framework for Kotlin coroutines on JVM.

Inspired by SparkJava, Jooby, etc, but [smaller, simpler and better](docs/Comparisons.md).

## Goals

* Proper Kotlin coroutine support with working before/after filters for e.g. transactions and logging
* Minimal amount of code
  * Simple to maintain & change
  * Performance is also important, but simplicity is preferred
* Zero dependencies - Java built-in **jdk.httpserver** is used under the hood
  * Perfect for microservices
  * But still possible to easily add support for other servers if needed
  * [Sample docker image](sample/Dockerfile) is about 50-70Mb thanks to jlink, depending on used modules
* 12-factor apps by default
* Most behaviour can be overridden if necessary
* Both route builder and annotated classes
* Very easy to do simple things, e.g.
  `@GET suspend fun route() = provider.fetchData()`
* Most app code will not depend on the framework, easy to switch
* Not much need for documentation - the source code is short and readable.

## Dependencies

* Java 6+ built-in non-blocking jdk.httpserver
* Re-routable Java 9+ System.Logger

# Modules

* [server](server) - the main server module. See [it's docs](server). Zero external dependencies.
* [jackson](jackson) - adds json parsing/rendering using Jackson
* [serialization](serialization) - adds json parsing/rendering using kotlinx-serialization
* [slf4j](slf4j) - redirects server logs to slf4j and configures it
* [i18n](i18n) - simple server-side translations (for emails, etc)
* [jdbc](jdbc) - provides jdbc extensions and transaction handling (depends on slf4j because of Hikari)
* [jdbc-test](jdbc-test) - provides a way of testing your DB code using a real DB
* [jobs](jobs) - provides a simple scheduled JobRunner
* [liquibase](liquibase) - allows to use liquibase for DB migration

## Status

The core server is less than 1000 lines of code.

Klite powers a few production apps already.
Public announcement at [KKON 2022](https://rheinwerk-kkon.de/programm/keks-klite/), see [the slides](https://docs.google.com/presentation/d/1m5UORE88nVRdZXyDEoj74c0alk1Ff_tX8mfB8oLMbk0).

## Usage

See [the sample subproject](sample) on how to build apps with Klite and run them in Docker.

Klite builds are available from [jitpack](https://jitpack.io/#codeborne/klite).

[![Release](https://jitpack.io/v/codeborne/klite.svg)](https://jitpack.io/#codeborne/klite)

```kotlin
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  dependencies {
    val kliteVersion = "master-SNAPSHOT" // you can put a released tag or commit hash here
    implementation("com.github.codeborne.klite:klite-server:$kliteVersion")
    // Plus any optional components with their own external dependencies, see above for list
    implementation("com.github.codeborne.klite:klite-jackson:$kliteVersion")
    implementation("com.github.codeborne.klite:klite-jdbc:$kliteVersion")
    testImplementation("com.github.codeborne.klite:klite-jdbc-test:$kliteVersion")
    ...
  }
```

### Using unreleased commits

Jitpack builds requested versions on the fly, so it is also good if you want to fork Klite and customize for your own needs -
you will still be able to add your fork as a Maven/Gradle dependency in your apps.

But pull-requests are welcome if you want to improve something for everybody!

### Depending on a local build

Publish to `~/.m2/repository` by running `./gradlew publishToMavenLocal`

Then add `mavenLocal()` repository to your project and use Klite version of `master-SNAPSHOT`.

### Depenending on the Git repository (source) directly

If there is a problem with Jitpack, then it's possible to add the following to your `settings.gradle.kts`:

```kotlin
sourceControl {
  gitRepository(java.net.URI("https://github.com/codeborne/klite.git")) {
    producesModule("com.github.codeborne.klite:server")
    producesModule("com.github.codeborne.klite:jdbc")
    // list all subprojects you depend on in build.gradle.kts, use their unprefixed names, e.g. server, not klite-server
  }
}
```

Gradle will clone and build Klite for you automatically during your project's build.
*Tagged version numbers* are supported this way, but *not commit hashes*.

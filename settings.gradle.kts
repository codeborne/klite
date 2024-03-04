rootProject.name = "klite"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("kotlin", "1.9.22")

      val coroutines = version("coroutines", "1.7.3")
      library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8").versionRef(coroutines)
      library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef(coroutines)
      library("kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
      library("kotlinx-datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

      val junit5 = version("junit", "5.10.1")
      library("junit", "org.junit.jupiter", "junit-jupiter").versionRef(junit5)
      library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef(junit5)
      library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef(junit5)
      library("atrium", "ch.tutteli.atrium:atrium-fluent:1.1.0")
      library("mockk", "io.mockk:mockk:1.13.9")

      val slf4j = version("slf4j", "2.0.9")
      library("slf4j-api", "org.slf4j", "slf4j-api").versionRef(slf4j)
      library("slf4j-jul", "org.slf4j", "jul-to-slf4j").versionRef(slf4j)

      val jackson = version("jackson", "2.16.0")
      library("jackson-jsr310", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef(jackson)
      library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef(jackson)

      library("hikari", "com.zaxxer:HikariCP:5.1.0")
      library("liquibase-core", "org.liquibase:liquibase-core:4.26.0")
      library("postgresql", "org.postgresql:postgresql:42.7.2")
    }
  }
}

include("core", "server", "json", "csv", "jackson", "i18n", "serialization", "jdbc", "jobs", "jdbc-test", "oauth", "liquibase", "slf4j", "openapi", "sample")

rootProject.name = "klite"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("kotlin", "1.8.21")

      val coroutines = version("coroutines", "1.6.4")
      library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8").versionRef(coroutines)
      library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef(coroutines)
      library("kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
      library("kotlinx-datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

      val junit5 = version("junit", "5.9.3")
      library("junit", "org.junit.jupiter", "junit-jupiter").versionRef(junit5)
      library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef(junit5)
      library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef(junit5)
      library("atrium", "ch.tutteli.atrium:atrium-fluent-en_GB:0.18.0")
      library("mockk", "io.mockk:mockk:1.13.5")

      val slf4j = version("slf4j", "2.0.7")
      library("slf4j-api", "org.slf4j", "slf4j-api").versionRef(slf4j)
      library("slf4j-jul", "org.slf4j", "jul-to-slf4j").versionRef(slf4j)

      val jackson = version("jackson", "2.15.0")
      library("jackson-jsr310", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef(jackson)
      library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef(jackson)

      library("hikari", "com.zaxxer:HikariCP:5.0.1")
      library("liquibase-core", "org.liquibase:liquibase-core:4.20.0")
      library("postgresql", "org.postgresql:postgresql:42.6.0")
    }
  }
}

include("core", "server", "json", "jackson", "i18n", "serialization", "jdbc", "jobs", "jdbc-test", "liquibase", "slf4j", "sample")

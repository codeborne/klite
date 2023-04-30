import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.21"
}

allprojects {
  group = "com.github.codeborne.klite"
  version = "master-SNAPSHOT" // see tags/releases
}

subprojects {
  apply(plugin = "kotlin")
  apply(plugin = "maven-publish")

  repositories {
    mavenCentral()
  }

  dependencies {
    val libs = rootProject.libs
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.atrium) {
      exclude("org.jetbrains.kotlin")
    }
    testImplementation(libs.mockk) {
      exclude("org.jetbrains.kotlin")
    }
    testImplementation(libs.kotlinx.coroutines.test)
  }

  sourceSets {
    main {
      java.setSrcDirs(emptyList<String>())
      kotlin.setSrcDirs(listOf("src"))
      resources.setSrcDirs(listOf("src")).exclude("**/*.kt")
    }
    test {
      java.setSrcDirs(emptyList<String>())
      kotlin.setSrcDirs(listOf("test"))
      resources.setSrcDirs(listOf("test")).exclude("**/*.kt")
    }
  }

  kotlin {
    jvmToolchain(11)
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs += "-opt-in=kotlin.ExperimentalStdlibApi"
    }
  }

  tasks.jar {
    archiveBaseName.set("${rootProject.name}-${project.name}")
    manifest {
      attributes(mapOf(
        "Implementation-Title" to archiveBaseName,
        "Implementation-Version" to project.version
      ))
    }
  }

  java {
    withSourcesJar()
  }

  tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  tasks.test {
    useJUnitPlatform()
    // enable JUnitAssertionImprover from klite.jdbc-test
    jvmArgs("-Djunit.jupiter.extensions.autodetection.enabled=true", "--add-opens=java.base/java.lang=ALL-UNNAMED")
  }

  // disable publishing gradle .modules files as JitPack omits excludes from there: https://github.com/jitpack/jitpack.io/issues/5349
  tasks.withType<GenerateModuleMetadata> {
    enabled = false
  }

  configure<PublishingExtension> {
    publications {
      if (project.name != "sample") {
        register<MavenPublication>("maven") {
          from(components["java"])
          afterEvaluate {
            artifactId = tasks.jar.get().archiveBaseName.get()
          }
        }
      }
    }
  }
}

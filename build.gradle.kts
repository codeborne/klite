import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.7.10"
}

allprojects {
  group = rootProject.name
  version = "1.0-SNAPSHOT"
}

subprojects {
  apply(plugin = "kotlin")
  apply(plugin = "maven-publish")

  repositories {
    mavenCentral()
  }

  dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("ch.tutteli.atrium:atrium-fluent-en_GB:0.17.0") {
      exclude("org.jetbrains.kotlin")
    }
    testImplementation("io.mockk:mockk:1.12.1") {
      exclude("org.jetbrains.kotlin")
    }
  }

  sourceSets {
    named("main") {
      java.srcDirs("src")
      resources.srcDirs("src").exclude("**/*.kt")
    }
    named("test") {
      java.srcDirs("test")
      resources.srcDirs("test").exclude("**/*.kt")
    }
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "11"
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


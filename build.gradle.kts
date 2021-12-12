import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.6.0"
}

allprojects {
  group = rootProject.name
  version = "0.5-SNAPSHOT"
}

subprojects {
  apply(plugin = "kotlin")
  apply(plugin = "maven-publish")

  repositories {
    mavenCentral()
  }

  dependencies {
    implementation("com.codeborne:jvm2dts:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("io.mockk:mockk:1.12.1")
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
      javaParameters = true
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

  tasks.test {
    useJUnitPlatform()
  }

  configure<PublishingExtension> {
    repositories {
      maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/angryziber/klite")
        credentials {
          username = System.getenv("GITHUB_ACTOR")
          password = System.getenv("GITHUB_TOKEN")
        }
      }
    }
    publications {
      if (project.name != "sample") {
        register<MavenPublication>("gpr") { from(components["kotlin"]) }
      }
    }
  }
}

plugins {
  kotlin("plugin.serialization") version "1.6.10"
}

dependencies {
  api(project(":server"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
}

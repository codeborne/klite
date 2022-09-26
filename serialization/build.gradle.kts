plugins {
  kotlin("plugin.serialization") version "1.7.10"
}

dependencies {
  api(project(":server"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
}

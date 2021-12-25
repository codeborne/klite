plugins {
  kotlin("plugin.serialization") version "1.6.10"
}

dependencies {
  api(project(":server"))
  api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

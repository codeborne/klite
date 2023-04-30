plugins {
  kotlin("plugin.serialization") version libs.versions.kotlin
}

dependencies {
  api(project(":server"))
  implementation(libs.kotlinx.serialization.json)
  testImplementation(libs.kotlinx.datetime)
}

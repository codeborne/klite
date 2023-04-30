dependencies {
  api(project(":server"))
  api(libs.jackson.jsr310)
  api(libs.jackson.kotlin) {
    exclude("org.jetbrains.kotlin")
  }
}

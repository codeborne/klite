dependencies {
  api(project(":server"))
  api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0")
  api("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0") {
    exclude("org.jetbrains.kotlin")
  }
}

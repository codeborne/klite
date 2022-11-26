dependencies {
  api(project(":server"))
  api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1")
  api("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.1") {
    exclude("org.jetbrains.kotlin")
  }
}

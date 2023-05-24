dependencies {
  api(project(":core"))
  compileOnly(project(":server"))
  testImplementation(project(":server"))
  compileOnly(libs.hikari) {
    exclude("org.slf4j")
  }
}

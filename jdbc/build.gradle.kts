dependencies {
  api(project(":core"))
  api(project(":slf4j"))
  compileOnly(project(":server"))
  compileOnly(libs.hikari) {
    exclude("org.slf4j")
  }
}

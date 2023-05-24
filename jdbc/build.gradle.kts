dependencies {
  api(project(":core"))
  api(project(":slf4j"))
  implementation(project(":server"))
  compileOnly(libs.hikari) {
    exclude("org.slf4j")
  }
}

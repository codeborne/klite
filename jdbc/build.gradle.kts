dependencies {
  api(project(":core"))
  api(project(":slf4j"))
  implementation(project(":server"))
  api(libs.hikari) {
    exclude("org.slf4j")
  }
}

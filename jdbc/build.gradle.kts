dependencies {
  api(project(":core"))
  api(project(":slf4j"))
  implementation(project(":server"))
  api("com.zaxxer:HikariCP:5.0.1") {
    exclude("org.slf4j")
  }
}

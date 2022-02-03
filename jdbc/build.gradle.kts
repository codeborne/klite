dependencies {
  api(project(":server"))
  api(project(":slf4j"))
  api("com.zaxxer:HikariCP:5.0.1") {
    exclude("org.slf4j")
  }
}

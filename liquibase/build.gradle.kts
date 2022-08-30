dependencies {
  implementation(project(":server"))
  api(project(":slf4j"))
  api("org.slf4j:jul-to-slf4j:2.0.0")
  api("org.liquibase:liquibase-core:4.15.0") {
    exclude("org.slf4j")
  }
}

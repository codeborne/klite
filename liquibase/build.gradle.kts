dependencies {
  implementation(project(":server"))
  api(project(":slf4j"))
  api("org.slf4j:jul-to-slf4j:1.7.33")
  api("org.liquibase:liquibase-core:4.7.1") {
    exclude("org.slf4j")
  }
}

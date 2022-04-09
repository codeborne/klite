dependencies {
  implementation(project(":server"))
  api(project(":slf4j"))
  api("org.slf4j:jul-to-slf4j:1.7.36")
  api("org.liquibase:liquibase-core:4.9.1") {
    exclude("org.slf4j")
  }
}

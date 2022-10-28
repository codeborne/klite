dependencies {
  implementation(project(":server"))
  api(project(":slf4j"))
  api("org.slf4j:jul-to-slf4j:2.0.3")
  api("org.liquibase:liquibase-core:4.17.1") {
    // include required sub-dependencies in your project if needed
    exclude("org.slf4j")
    exclude("com.opencsv")
    exclude("javax.xml.bind")
    exclude("org.yaml")
  }
}

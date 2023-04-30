dependencies {
  implementation(project(":server"))
  implementation(project(":slf4j"))
  implementation("org.slf4j:jul-to-slf4j:2.0.7")
  api("org.liquibase:liquibase-core:4.20.0") {
    exclude("javax.xml.bind")
    exclude("org.yaml")
    exclude("com.opencsv")
  }
}

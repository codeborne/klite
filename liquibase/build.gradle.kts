dependencies {
  implementation(project(":server"))
  api(project(":slf4j"))
  api("org.slf4j:jul-to-slf4j:2.0.5")
  api("org.liquibase:liquibase-core:4.17.2") {
    // TODO: unfortunately these are not propagated to projects depending on klite-liquibase...
    exclude("org.slf4j")
    exclude("com.opencsv")
    exclude("javax.xml.bind")
    exclude("org.yaml")
  }
}

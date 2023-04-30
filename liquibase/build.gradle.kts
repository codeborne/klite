dependencies {
  implementation(project(":server"))
  implementation(project(":slf4j"))
  implementation(libs.slf4j.jul)
  api(libs.liquibase.core) {
    exclude("javax.xml.bind")
    exclude("org.yaml")
    exclude("com.opencsv")
  }
}

dependencies {
  implementation(project(":server"))
  implementation(project(":slf4j"))
  implementation("org.slf4j:jul-to-slf4j:2.0.5")
  api("org.liquibase:liquibase-core:4.18.0") {
    // TODO: unfortunately excludes are not currently published by Jitpack: https://github.com/jitpack/jitpack.io/issues/5349
    exclude("javax.xml.bind")
    exclude("org.yaml")
    exclude("com.opencsv")
  }
}

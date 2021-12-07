plugins {
  application
}

dependencies {
  implementation(project(":server"))
  implementation(project(":jackson"))
  implementation(project(":jdbc"))
  implementation("org.postgresql:postgresql:42.3.1")
}

application {
  applicationDefaultJvmArgs += "--add-exports=java.base/sun.net.www=ALL-UNNAMED"
  mainClass.set("LauncherKt")
}

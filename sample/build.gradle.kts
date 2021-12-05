plugins {
  application
}

dependencies {
  implementation(project(":server"))
  implementation(project(":jackson"))
}

application {
  applicationDefaultJvmArgs += "--add-exports=java.base/sun.net.www=ALL-UNNAMED"
  mainClass.set("LauncherKt")
}

plugins {
  application
}

dependencies {
  implementation(project(":server"))
}

application {
  applicationDefaultJvmArgs += "--add-exports=java.base/sun.net.www=ALL-UNNAMED"
  mainClass.set("LauncherKt")
}

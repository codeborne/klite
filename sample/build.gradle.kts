plugins {
  application
}

dependencies {
  implementation(project(":core"))
}

application {
  applicationDefaultJvmArgs += "--add-exports=java.base/sun.net.www=ALL-UNNAMED"
  mainClass.set("LauncherKt")
}

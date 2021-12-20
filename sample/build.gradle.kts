val mainClassName = "LauncherKt"

dependencies {
  implementation(project(":server"))
  implementation(project(":jackson"))
  implementation(project(":jdbc"))
  implementation(project(":liquibase"))
  implementation(project(":slf4j"))
  implementation("org.postgresql:postgresql:42.3.1")
  testImplementation(project(":jdbc-test"))
}

sourceSets {
  named("main") {
    resources.srcDir("db")
  }
}

tasks.register<Copy>("deps") {
  into("$buildDir/libs/deps")
  from(configurations.runtimeClasspath)
}

tasks.jar {
  dependsOn("deps")
  doFirst {
    manifest {
      attributes(
        "Main-Class" to mainClassName,
        "Class-Path" to File("$buildDir/libs/deps").listFiles()?.joinToString(" ") { "deps/${it.name}"}
      )
    }
  }
}

tasks.register<JavaExec>("run") {
  jvmArgs("--add-exports=java.base/sun.net.www=ALL-UNNAMED")
  mainClass.set(mainClassName)
  classpath = sourceSets.main.get().runtimeClasspath
}

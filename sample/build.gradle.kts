val mainClassName = "LauncherKt"

dependencies {
  implementation(project(":server"))
  implementation(project(":jackson"))
  implementation(project(":i18n"))
  implementation(project(":jdbc"))
  implementation(project(":slf4j"))
  implementation("org.postgresql:postgresql:42.5.1")
  testImplementation(project(":jdbc-test"))
}

sourceSets {
  named("main") {
    resources.srcDirs("db", "i18n")
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
  mainClass.set(mainClassName)
  classpath = sourceSets.main.get().runtimeClasspath
}

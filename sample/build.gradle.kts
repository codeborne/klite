import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

val mainClassName = "LauncherKt"

dependencies {
  implementation(project(":server"))
  implementation(project(":json"))
  implementation(project(":i18n"))
  implementation(project(":jdbc"))
  implementation(project(":slf4j"))
  implementation("org.postgresql:postgresql:42.5.1")
  testImplementation(project(":jdbc-test"))
}

sourceSets {
  main {
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

tasks.register("types.ts") {
  dependsOn("classes")
  doLast {
    val mainSource = sourceSets.main.get()
    project.file("build/types.ts").writeText(ByteArrayOutputStream().use { out ->
      project.javaexec {
        standardOutput = out
        mainClass.set("klite.json.TSGeneratorKt")
        classpath = sourceSets.main.get().runtimeClasspath
        args("${project.buildDir}/classes/kotlin/main")
      }
      out.toString()
    })
  }
}

tasks.withType<KotlinCompile> {
  finalizedBy("types.ts")
}

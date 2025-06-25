plugins {
    kotlin("jvm") version "2.1.20"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

val appName = "MonkeySorter"

application {
    mainClass.set("com.segerend.MainKt")
//    val osName = System.getProperty("os.name").lowercase()
}

group = "org.segerend"
version = "1.0.0"

repositories {
    mavenCentral()
}

val javafxVersion = "21"

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-fxml:$javafxVersion")
    implementation("org.openjfx:javafx-media:$javafxVersion")
    implementation("org.openjfx:javafx-graphics:$javafxVersion") // Explcit import, already included in javafx-controls
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.media", "javafx.graphics")
}

tasks.register<Jar>("fatJar") {
    group = "build"
    dependsOn("classes")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE  // <--- add this line here
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.register<Exec>("packageApp") {
    group = "distribution"
    dependsOn("fatJar")

    val buildDirFile = layout.buildDirectory.get().asFile
    val jpackageOutput = File(buildDirFile, "dist")
    val appOutputFolder = File(jpackageOutput, "$appName.app")

    val fatJarTask = tasks.named<Jar>("fatJar").get()
    val jarFileName = fatJarTask.archiveFileName.get()

    val osName = System.getProperty("os.name").lowercase()

    doFirst {
        jpackageOutput.mkdirs()
        if (appOutputFolder.exists()) {
            println("Deleting existing application folder: ${appOutputFolder.absolutePath}")
            appOutputFolder.deleteRecursively()
        }
    }

    val command = mutableListOf(
        "jpackage",
        "--type", "app-image",
        "--input", File(buildDirFile, "libs").absolutePath,
        "--name", appName,
        "--main-jar", jarFileName,
        "--main-class", "com.segerend.MainKt",
        "--dest", jpackageOutput.absolutePath,
    )

    if (osName.contains("mac")) {
        command += listOf(
//            "--icon", "src/main/resources/icon.icns",
            "--app-version", version.toString(),
            "--mac-package-identifier", "com.segerend.monkeysorter",
            "--mac-package-name", appName,
            "--vendor", "SegerEnd",
            // Add other mac options here like --mac-sign etc.
        )
    }

    commandLine(command)

    doLast {
        println("🎉 Application image created at: ${appOutputFolder.absolutePath}")
    }
}

tasks.named("build") {
    dependsOn("packageApp")
}

tasks.register<Exec>("runApp") {
    group = "application"
    dependsOn("packageApp")

    val jpackageOutput = File(layout.buildDirectory.get().asFile, "dist")

    doFirst {
        val osName = System.getProperty("os.name").lowercase()
        val command: List<String> = when {
            osName.contains("mac") -> {
                val appPath = File(jpackageOutput, "$appName.app")
                if (!appPath.exists()) {
                    throw GradleException("App bundle not found at $appPath")
                }
                listOf("open", appPath.absolutePath)
            }
            osName.contains("windows") -> {
                val exePath = File(jpackageOutput, "$appName.exe")
                if (!exePath.exists()) {
                    throw GradleException("Executable not found at $exePath")
                }
                listOf(exePath.absolutePath)
            }
            else -> {
                throw GradleException("Unsupported OS for runApp task")
            }
        }
        commandLine(command)
    }
}
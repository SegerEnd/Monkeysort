plugins {
    kotlin("jvm") version "2.1.20"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

val appName = "Monkeysort"

application {
    mainClass.set("com.segerend.MainKt")
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("mac")) {
        applicationDefaultJvmArgs = listOf("-Xdock:name=$appName")
    }
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
    implementation("org.openjfx:javafx-graphics:$javafxVersion")
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

// Create a fat JAR
tasks.register<Jar>("fatJar") {
    group = "build"
    dependsOn("classes")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

// Package native app
tasks.register<Exec>("packageApp") {
    group = "distribution"
    dependsOn("fatJar")

    val buildDirFile = layout.buildDirectory.get().asFile
    val jpackageOutput = File(buildDirFile, "dist")
    val fatJarTask = tasks.named<Jar>("fatJar").get()
    val jarFileName = fatJarTask.archiveFileName.get()
    val osName = System.getProperty("os.name").lowercase()

    doFirst {
        jpackageOutput.mkdirs()
        jpackageOutput.listFiles()?.forEach { it.deleteRecursively() }
    }

    val commonArgs = mutableListOf(
        "jpackage",
        "--type", "app-image",
        "--input", File(buildDirFile, "libs").absolutePath,
        "--name", appName,
        "--main-jar", jarFileName,
        "--main-class", application.mainClass.get(),
        "--dest", jpackageOutput.absolutePath,
        "--app-version", version.toString(),
        "--vendor", "SegerEnd"
    )

    when {
        osName.contains("mac") -> {
            commonArgs += listOf(
                "--mac-package-identifier", "com.segerend.${appName.lowercase().replace(" ", "")}",
                "--mac-package-name", appName
                // Optionally add icon for macOS
            )
        }
//        osName.contains("windows") -> {
//            commonArgs += listOf(
//            )
//        }
    }

    commandLine(commonArgs)

    doLast {
        println("🎉 App image created at: ${jpackageOutput.absolutePath}")
    }
}

// Run packaged app
tasks.register<Exec>("runApp") {
    group = "application"
    dependsOn("packageApp")

    val jpackageOutput = File(layout.buildDirectory.get().asFile, "dist")

    doFirst {
        val osName = System.getProperty("os.name").lowercase()
        val command: List<String> = when {
            osName.contains("mac") -> {
                val appPath = File(jpackageOutput, "$appName.app")
                if (!appPath.exists()) throw GradleException("App bundle not found at $appPath")
                listOf("open", appPath.absolutePath)
            }
            osName.contains("windows") -> {
                val exePath = File(jpackageOutput, "$appName/$appName.exe")
                if (!exePath.exists()) throw GradleException("Executable not found at $exePath")
                listOf(exePath.absolutePath)
            }
            else -> throw GradleException("Unsupported OS for runApp task")
        }
        commandLine(command)
    }
}

tasks.named("build") {
    dependsOn("packageApp")
}

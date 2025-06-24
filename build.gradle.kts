plugins {
    kotlin("jvm") version "2.1.20"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

application {
    mainClass.set("com.segerend.MainKt")
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("mac")) {
        applicationDefaultJvmArgs = listOf("-Xdock:name=SegerEnd App")
//        applicationDefaultJvmArgs += "-Xdock:icon=src/main/resources/icon.png"
    }
}

group = "org.segerend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val javafxVersion = "21"
//val osName = System.getProperty("os.name").lowercase()
//val javafxplatform = when {
//    osName.contains("mac") -> "mac"
//    osName.contains("win") -> "" // Windows does not require a specific platform suffix for JavaFX
//    else -> "linux"
//}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-fxml:$javafxVersion")
    implementation("org.openjfx:javafx-media:$javafxVersion")
    implementation("org.openjfx:javafx-graphics:$javafxVersion") // Explcit import, already included in javafx-controls

    // more useful libraries for JavaFX and making the app look better, game development, etc.
    // for swiftui look

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
plugins {
    kotlin("jvm") version "2.1.20"
    application
}

group = "org.segerend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // JavaFX modules - use appropriate version and platform classifier
    val javafxVersion = "21"

    implementation("org.openjfx:javafx-base:$javafxVersion:${osClassifier()}")
    implementation("org.openjfx:javafx-controls:$javafxVersion:${osClassifier()}")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:${osClassifier()}")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:${osClassifier()}")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.segerend.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// Helper function to detect OS for JavaFX native libs
fun osClassifier(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("mac") -> "mac"
        osName.contains("win") -> "win"
        osName.contains("linux") -> "linux"
        else -> throw GradleException("Unknown OS: $osName")
    }
}

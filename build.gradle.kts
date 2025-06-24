plugins {
    kotlin("jvm") version "2.1.20"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

application {
    mainClass.set("com.segerend.MainKt")
}

group = "org.segerend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val javafxVersion = "21"
val osName = System.getProperty("os.name").lowercase()
val javafxplatform = when {
    osName.contains("mac") -> "mac"
    osName.contains("win") -> ""
    else -> "linux"
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxplatform")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$javafxplatform")
    implementation("org.openjfx:javafx-media:$javafxVersion:$javafxplatform")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

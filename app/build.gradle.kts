/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("learn.arrow.kotlin.kotlin-application-conventions")
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation("io.arrow-kt:arrow-core:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

application {
    // Define the main class for the application.
    mainClass.set("learn.arrow.kotlin.app.AppKt")
}

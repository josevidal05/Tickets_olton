// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // This line already includes "com.android.application" via your libs.versions.toml
    alias(libs.plugins.android.application) apply false

    // REMOVED: id("com.android.application") ... (This was the duplicate)

    // Keep these if they are not defined in your libs.versions.toml yet
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

// Asegúrate de que los repositorios estén configurados (normalmente van en settings.gradle.kts,
// pero en versiones antiguas o configuraciones específicas se pueden necesitar aquí)
buildscript {
    repositories {
        google()
        mavenCentral()
    }
}
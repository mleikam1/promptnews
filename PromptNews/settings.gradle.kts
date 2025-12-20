pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // AGP aligned with Gradle 8.13
        id("com.android.application") version "8.8.0"
        // Kotlin + Compose
        id("org.jetbrains.kotlin.android") version "2.0.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PromptNews"
include(":app")

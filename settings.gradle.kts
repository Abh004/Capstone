pluginManagement {
    repositories {
        google() // Simplified to ensure all Google-signed plugins are found
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()       // Required for com.google.ai.edge.litert
        mavenCentral()
    }
}

rootProject.name = "testapp"
include(":app")
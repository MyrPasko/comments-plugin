import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "comments-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.3.0"
        id("org.jetbrains.intellij.platform") version "2.16.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

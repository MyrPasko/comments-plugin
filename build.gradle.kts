plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

group = "com.myrpasko"
version = "0.1.0"

val localIdePath = providers.gradleProperty("localIdePath")
    .orElse(providers.environmentVariable("LOCAL_IDE_PATH"))
    .orNull

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        if (localIdePath != null) {
            local(localIdePath)
        } else {
            intellijIdea("2025.2.6.2")
        }
        bundledPlugins("Git4Idea", "org.jetbrains.plugins.terminal")
        pluginVerifier()
        zipSigner()
    }

    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

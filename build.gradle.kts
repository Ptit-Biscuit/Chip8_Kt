import org.gradle.internal.os.OperatingSystem

val openrndrVersion = "0.3.42-rc.5"
val openrndrOs = when (OperatingSystem.current()) {
    OperatingSystem.WINDOWS -> "windows"
    OperatingSystem.MAC_OS -> "macos"
    OperatingSystem.LINUX -> "linux-x64"
    else -> throw IllegalArgumentException("os not supported")
}

plugins {
    kotlin("jvm") version "1.4.21"
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/openrndr/openrndr")
}

fun openrndr(module: String): Any {
    return "org.openrndr:openrndr-$module:$openrndrVersion"
}

fun openrndrNatives(module: String): Any {
    return "org.openrndr:openrndr-$module-natives-$openrndrOs:$openrndrVersion"
}

kotlin {
    sourceSets {
        val main by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))

                implementation(openrndr("gl3"))
                implementation(openrndrNatives("gl3"))
                implementation(openrndr("core"))
                implementation(openrndr("extensions"))

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")

                implementation("io.github.microutils:kotlin-logging:1.7.2")

                implementation("org.slf4j:slf4j-nop:1.7.25")
            }
        }

        main.kotlin.srcDir("src/main/kotlin")

        val test by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
            }
        }

        test.kotlin.srcDir("test/kotlin")
    }
}
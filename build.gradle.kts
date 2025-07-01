plugins {

    val kotlinVer = System.getenv("KOTLIN_VERSION_ENV")?.ifBlank { null } ?: libs.versions.kotlin.get()
    val kotestVer = System.getenv("KOTEST_VERSION_ENV")?.ifBlank { null } ?: libs.versions.kotest.get()

    id("at.asitplus.gradle.conventions") version "20250628"
    id("io.kotest.multiplatform") version kotestVer
    kotlin("multiplatform") version kotlinVer apply false
    kotlin("plugin.serialization") version kotlinVer apply false
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)

    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) version (vckOidCatalog.versions.kotlin) apply false
}

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

allprojects {
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}


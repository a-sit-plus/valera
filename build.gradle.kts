plugins {
    val kotlinVer =
        System.getenv("KOTLIN_VERSION_ENV")?.ifBlank { null } ?: libs.versions.kotlin.get()
    val testballoonVer =
        System.getenv("TESTBALLOON_VERSION_OVERRIDE")?.ifBlank { null } ?: libs.versions.testballoon.get()

    id("at.asitplus.gradle.conventions") version "20251023"
    kotlin("multiplatform") version kotlinVer apply false
    kotlin("plugin.serialization") version kotlinVer apply false
    id("de.infix.testBalloon") version testballoonVer apply false

    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("com.android.application") version libs.versions.agp.get() apply (false)
    id("com.android.kotlin.multiplatform.library") version libs.versions.agp.get() apply (false)

    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) version kotlinVer apply false
}

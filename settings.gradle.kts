pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

rootProject.name = "compose-wallet-app"

include(":androidApp")
include(":shared")
include(":cinterop")
include(":interop")

val vckDir = file("../vck")
val vckBuildFile = file("../vck/build.gradle.kts")
if (vckDir.isDirectory && vckBuildFile.exists()) {
    logger.warn("Detected VC-K in ${vckDir.absolutePath}.")
    logger.warn("Including VC-K as composite build.")
    logger.warn("If you do not want this, move the VC-K to another location!")
    includeBuild("../vck")
}

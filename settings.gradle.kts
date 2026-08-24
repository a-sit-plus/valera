import java.util.Properties

include(":androidApp")
include(":shared")

val localProperties = Properties().apply {
    rootDir.resolve("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}
val disableAppleTargets = System.getenv("disableAppleTargets")
    ?: localProperties.getProperty("disableAppleTargets")

if ("true" != disableAppleTargets) {
    include(":cinterop")
    include(":interop")
}

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

val vckDir = System.getenv("VCK_COMPOSITE_PATH")
    ?.takeIf { it.isNotBlank() }
    ?.let(::file)
    ?: file("../vck")
val vckBuildFile = vckDir.resolve("build.gradle.kts")
if (vckDir.isDirectory && vckBuildFile.exists()) {
    logger.warn("Detected VC-K in ${vckDir.absolutePath}.")
    logger.warn("Including VC-K as composite build.")
    logger.warn("If you do not want this, move the VC-K to another location!")
    includeBuild(vckDir)
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

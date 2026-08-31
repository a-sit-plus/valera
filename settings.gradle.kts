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

// Mirrors the conventions plugin's `envExtra` lookup, which is not available this early:
// environment first, then -P properties, then gradle.properties overridden by local.properties.
val disableAppleTargets: String? = System.getenv("disableAppleTargets")
    ?: startParameter.projectProperties["disableAppleTargets"]
    ?: run {
        val properties = java.util.Properties()
        listOf("gradle.properties", "local.properties").forEach { name ->
            val file = File(rootDir, name)
            if (file.exists()) file.inputStream().use { properties.load(it) }
        }
        properties.getProperty("disableAppleTargets")
    }

include(":androidApp")
include(":shared")

// `cinterop` and `interop` exist only to bridge the Apple Digital Credentials API. Leaving them out
// entirely when Apple targets are disabled avoids a Kotlin module that declares no targets at all,
// which KGP rejects. `shared` guards its `iosMain` dependency on `:interop` with the same flag.
if ("true" != disableAppleTargets) {
    include(":cinterop")
    include(":interop")
}

val vckDir = file("../vck")
val vckBuildFile = file("../vck/build.gradle.kts")
if (vckDir.isDirectory && vckBuildFile.exists()) {
    logger.warn("Detected VC-K in ${vckDir.absolutePath}.")
    logger.warn("Including VC-K as composite build.")
    logger.warn("If you do not want this, move the VC-K to another location!")
    includeBuild("../vck")
}

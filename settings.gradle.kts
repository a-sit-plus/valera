include(":androidApp")
include(":shared")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}


val vckDir= file("../vck")
val signumFile=file("../vck/signum/build.gradle.kts")
if (vckDir.isDirectory && signumFile.exists()) {
    logger.warn("Detected VC-K in ${vckDir.absolutePath}.")
    logger.warn("Including VC-K and Signum as composite build.")
    logger.warn("If you do not want this, move the VC-K to another location!")
    includeBuild("../vck")
}

dependencyResolutionManagement {

    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
